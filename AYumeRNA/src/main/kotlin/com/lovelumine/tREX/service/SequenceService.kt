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

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Aligning and scoring test sequences...")

        val results = scoreTestSequences(
            alignedThrSequences,
            conservedPositions,
            testSequences,
            userId
        )

        messagingTemplate.convertAndSend("/topic/progress/$userId", "Progress: 100% - Alignment completed")
        val resultsCsvPath = Paths.get(tempDirPath, "results.csv")
        saveResultsToCsv(results, resultsCsvPath.toString())

        return resultsCsvPath
    }

    private fun scoreTestSequences(
        alignedThrSequences: Map<String, String>,
        conservedPositions: Set<Int>,
        testSequences: List<Pair<String, RNASequence>>,
        userId: Long
    ): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val gapPenalty = -2

        val thrSeqs = alignedThrSequences.map { (header, seqStr) ->
            val seqWithoutGaps = seqStr.replace('-', 'N')
            val rnaSeq = RNASequence(seqWithoutGaps)
            rnaSeq.accession = AccessionID(header)
            rnaSeq
        }

        for ((index, testPair) in testSequences.withIndex()) {
            val testHeader = testPair.first
            val testSeq = testPair.second

            try {
                testSeq.accession = AccessionID(testHeader)

                val combinedSequences = mutableListOf<RNASequence>()
                combinedSequences.addAll(thrSeqs)
                combinedSequences.add(testSeq)

                val gapPenaltyObj = SimpleGapPenalty(5, 2)
                val substitutionMatrix = getRnaSubstitutionMatrix()

                val msa: Profile<RNASequence, NucleotideCompound> = Alignments.getMultipleSequenceAlignment(
                    combinedSequences,
                    Alignments.PairwiseSequenceAlignerType.GLOBAL,
                    substitutionMatrix,
                    gapPenaltyObj
                )

                val alignedSequences = mutableMapOf<String, String>()
                msa.alignedSequences.forEach { alignedSeq ->
                    val header = alignedSeq.originalSequence.accession.id
                    val seq = alignedSeq.toString()
                    alignedSequences[header] = seq
                }

                val alignedTestSeq = alignedSequences[testHeader]

                if (alignedTestSeq == null) {
                    logger.error("Could not find aligned test sequence: $testHeader")
                    continue
                }

                var score = 0.0
                for (pos in conservedPositions) {
                    val testBase = alignedTestSeq[pos]
                    if (testBase == '-') {
                        score += gapPenalty
                        continue
                    }

                    val conservedBase = alignedThrSequences.values.first()[pos]
                    if (testBase == conservedBase) {
                        score += 1
                    } else {
                        score -= 1
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
