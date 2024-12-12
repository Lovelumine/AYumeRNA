package com.lovelumine.tREX.service

import com.lovelumine.tREX.model.SequenceTask
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.biojava.nbio.alignment.Alignments
import org.biojava.nbio.alignment.SimpleGapPenalty
import org.biojava.nbio.core.alignment.matrices.SimpleSubstitutionMatrix
import org.biojava.nbio.core.alignment.template.Profile
import org.biojava.nbio.core.alignment.template.SubstitutionMatrix
import org.biojava.nbio.core.sequence.AccessionID
import org.biojava.nbio.core.sequence.RNASequence
import org.biojava.nbio.core.sequence.compound.NucleotideCompound
import org.biojava.nbio.core.sequence.compound.RNACompoundSet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Comparator

@Service
class SequenceService(
    @Autowired private val minioClient: MinioClient,
    @Autowired private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${minio.url}") private val minioBaseUrl: String,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {

    private val logger = LoggerFactory.getLogger(SequenceService::class.java)

    fun processSequences(task: SequenceTask) {
        val userId = task.userId

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Task started")

        val tempDir = Files.createTempDirectory("sequence_upload_${userId}")
        val templateFilePath = tempDir.resolve("template.csv")
        val testFilePath = tempDir.resolve("test.fasta")

        Files.write(templateFilePath, task.templateFileData)
        Files.write(testFilePath, task.testFileData)

        try {
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Progress: 10% - Reading template sequences...")
            val resultsCsvPath = executeSequenceProcessing(
                templateFilePath.toString(),
                testFilePath.toString(),
                tempDir.toString(),
                userId
            )
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Sequences aligned, saving results...")

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val objectName = "$userId-$timestamp-result.csv"

            val resultsBytes = Files.readAllBytes(resultsCsvPath)
            val inputStream = ByteArrayInputStream(resultsBytes)
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, resultsBytes.size.toLong(), -1)
                    .contentType("text/csv")
                    .build()
            )

            val fileUrl = "$minioBaseUrl/$bucketName/$objectName"
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Task completed, results uploaded: $fileUrl")

        } catch (e: Exception) {
            logger.error("Task execution failed: ${e.message}", e)
            messagingTemplate.convertAndSend("/topic/progress/$userId", "Task failed: ${e.message}")
        } finally {
            Files.deleteIfExists(templateFilePath)
            Files.deleteIfExists(testFilePath)
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }

    private fun executeSequenceProcessing(
        templateFilePath: String,
        testFilePath: String,
        tempDirPath: String,
        userId: Long
    ): java.nio.file.Path {
        messagingTemplate.convertAndSend("/topic/progress/$userId", "Reading template sequences...")
        val thrSequences = readCsvSequences(templateFilePath)
        messagingTemplate.convertAndSend("/topic/progress/$userId", "Read ${thrSequences.size} template sequences.")

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Reading test sequences...")
        val testSequences = readFastaSequencesWithCCA(testFilePath)
        messagingTemplate.convertAndSend("/topic/progress/$userId", "Read ${testSequences.size} test sequences.")

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Aligning template sequences...")
        val (alignedThrSequences, conservedPositions) = performMultipleSequenceAlignment(thrSequences)
        messagingTemplate.convertAndSend("/topic/progress/$userId", "Template sequence alignment completed.")

        // 生成共识序列(以便对测试序列进行快速对齐及打分)
        val consensusSequence = generateConsensusSequence(alignedThrSequences)

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Aligning and scoring test sequences...")

        val results = scoreTestSequences(
            conservedPositions,
            testSequences,
            consensusSequence,
            userId
        )

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Progress: 100% - Alignment completed")
        val resultsCsvPath = Paths.get(tempDirPath, "results.csv")
        saveResultsToCsv(results, resultsCsvPath.toString())

        return resultsCsvPath
    }

    /**
     * 根据模板序列的对齐结果生成共识序列。
     * 共识序列长度等于对齐结果中序列的长度。
     * 对于保守位点，所有序列的该位点应当为相同的碱基，直接使用即可。
     * 对于非保守位点，可以用'N'或最常见的碱基作为共识（此处使用'N'简化处理）。
     */
    private fun generateConsensusSequence(alignedThrSequences: Map<String, String>): RNASequence {
        val sequenceLength = alignedThrSequences.values.first().length
        val consensusBuilder = StringBuilder()
        val seqValues = alignedThrSequences.values.toList()

        for (i in 0 until sequenceLength) {
            val columnChars = seqValues.map { it[i] }
            val filteredChars = columnChars.filter { it != '-' }

            val consensusChar = if (filteredChars.isEmpty()) {
                'N' // 无实际碱基信息，用N代替
            } else {
                // 如果都相同，取其一
                val distinct = filteredChars.toSet()
                if (distinct.size == 1) {
                    distinct.first()
                } else {
                    // 如果不保守，不做精细统计，直接用N表示不确定
                    'N'
                }
            }
            consensusBuilder.append(consensusChar)
        }

        return RNASequence(consensusBuilder.toString())
    }

    private fun scoreTestSequences(
        conservedPositions: Set<Int>,
        testSequences: List<Pair<String, RNASequence>>,
        consensusSequence: RNASequence,
        userId: Long
    ): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val gapPenalty = -2
        val substitutionMatrix = getRnaSubstitutionMatrix()
        val gapPenaltyObj = SimpleGapPenalty(5, 2)

        consensusSequence.accession = AccessionID("consensus")

        for ((index, testPair) in testSequences.withIndex()) {
            val testHeader = testPair.first
            val testSeq = testPair.second
            testSeq.accession = AccessionID(testHeader)

            try {
                // 调用时注意参数顺序
                val pairwiseResult = Alignments.getPairwiseAlignment(
                    consensusSequence,
                    testSeq,
                    Alignments.PairwiseSequenceAlignerType.GLOBAL,
                    gapPenaltyObj,      // gap penalty参数在此处
                    substitutionMatrix   // substitution matrix参数在最后
                )

                // 使用 alignedSequences 获取对齐结果
                val alignedCons = pairwiseResult.alignedSequences[0].toString()
                val alignedTest = pairwiseResult.alignedSequences[1].toString()

                var score = 0.0
                for (pos in conservedPositions) {
                    if (pos >= alignedCons.length || pos >= alignedTest.length) continue
                    val testBase = alignedTest[pos]
                    val consensusBase = alignedCons[pos]

                    score += when {
                        testBase == '-' -> gapPenalty
                        testBase == consensusBase -> 1
                        else -> -1
                    }
                }

                val normalizedScore = score / conservedPositions.size
                results.add(
                    mapOf(
                        "Test_Sequence" to testHeader,
                        "Score" to normalizedScore,
                        "Sequence" to testSeq.sequenceAsString
                    )
                )

                val progress = ((index + 1).toDouble() / testSequences.size * 80).toInt() + 10
                messagingTemplate.convertAndSend(
                    "/topic/progress/$userId",
                    "Progress: $progress% - Processed test sequence: $testHeader (${index + 1}/${testSequences.size})"
                )

            } catch (e: Exception) {
                logger.error("Alignment failed: ${e.message}", e)
                messagingTemplate.convertAndSend("/topic/progress/$userId", "Alignment failed: $testHeader, error: ${e.message}")
                continue
            }
        }

        return results
    }
    private fun readCsvSequences(filePath: String): List<Pair<String, RNASequence>> {
        val sequences = mutableListOf<Pair<String, RNASequence>>()
        Files.newBufferedReader(Paths.get(filePath)).use { reader ->
            reader.readLine() // Skip header
            var index = 0
            reader.lineSequence().forEach { line ->
                val columns = line.split(",")
                if (columns.isNotEmpty()) {
                    val seqStr = columns.last().trim()
                    val seq = RNASequence(seqStr)
                    val header = "Thr_seq_$index"
                    sequences.add(Pair(header, seq))
                    index++
                }
            }
        }
        return sequences
    }

    private fun readFastaSequencesWithCCA(filePath: String): List<Pair<String, RNASequence>> {
        val sequences = mutableListOf<Pair<String, RNASequence>>()
        var header: String? = null
        val seqBuilder = StringBuilder()
        Files.newBufferedReader(Paths.get(filePath)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith(">")) {
                    if (header != null && seqBuilder.isNotEmpty()) {
                        val sequence = seqBuilder.toString()
                        val rnaSeq = RNASequence(sequence)
                        sequences.add(Pair(header!!, rnaSeq))
                        seqBuilder.clear()
                    }
                    header = line.trim().removePrefix(">")
                } else {
                    seqBuilder.append(line.trim())
                }
            }
            if (header != null && seqBuilder.isNotEmpty()) {
                val sequence = seqBuilder.toString()
                val rnaSeq = RNASequence(sequence)
                sequences.add(Pair(header!!, rnaSeq))
            }
        }
        return sequences
    }

    private fun getRnaSubstitutionMatrix(): SubstitutionMatrix<NucleotideCompound> {
        val rnaCompounds = RNACompoundSet.getRNACompoundSet()
        val matchScore: Short = 1
        val mismatchScore: Short = -1
        return SimpleSubstitutionMatrix(rnaCompounds, matchScore, mismatchScore)
    }

    private fun performMultipleSequenceAlignment(
        sequences: List<Pair<String, RNASequence>>
    ): Pair<Map<String, String>, Set<Int>> {
        val rnaSequences = sequences.map { (header, seq) ->
            seq.accession = AccessionID(header)
            seq
        }

        val gapPenalty = SimpleGapPenalty(5, 2)
        val substitutionMatrix = getRnaSubstitutionMatrix()

        val msa: Profile<RNASequence, NucleotideCompound> = Alignments.getMultipleSequenceAlignment(
            rnaSequences,
            Alignments.PairwiseSequenceAlignerType.GLOBAL,
            substitutionMatrix,
            gapPenalty
        )

        val alignedSequences = mutableMapOf<String, String>()
        msa.alignedSequences.forEach { alignedSeq ->
            val header = alignedSeq.originalSequence.accession.id
            val seq = alignedSeq.toString()
            alignedSequences[header] = seq
        }

        val sequenceLength = alignedSequences.values.first().length
        val conservedPositions = mutableSetOf<Int>()
        for (i in 0 until sequenceLength) {
            val basesAtPosition = alignedSequences.values.map { it[i] }.toSet()
            if (basesAtPosition.size == 1 && basesAtPosition.first() != '-') {
                conservedPositions.add(i)
            }
        }

        return Pair(alignedSequences, conservedPositions)
    }

    private fun saveResultsToCsv(results: List<Map<String, Any>>, filePath: String) {
        val file = File(filePath)
        file.bufferedWriter().use { writer ->
            writer.write("Test_Sequence,Score,Sequence\n")
            results.sortedByDescending { it["Score"] as Double }
                .forEach { result ->
                    val testSeq = result["Test_Sequence"]
                    val score = result["Score"]
                    val sequence = result["Sequence"]
                    writer.write("$testSeq,$score,$sequence\n")
                }
        }
    }
}