package com.lovelumine.tREX.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

// 正确的导入
import org.biojava.nbio.alignment.Alignments
import org.biojava.nbio.alignment.SimpleGapPenalty
import org.biojava.nbio.core.alignment.template.Profile
import org.biojava.nbio.core.alignment.template.SubstitutionMatrix
import org.biojava.nbio.core.alignment.matrices.SimpleSubstitutionMatrix
import org.biojava.nbio.core.sequence.compound.NucleotideCompound
import org.biojava.nbio.core.sequence.compound.RNACompoundSet
import org.biojava.nbio.core.sequence.AccessionID
import org.biojava.nbio.core.sequence.RNASequence

@Service
class SequenceService {

    private val logger = LoggerFactory.getLogger(SequenceService::class.java)

    fun processSequences(
        templateFilePath: String,
        testFilePath: String,
        tempDirPath: String
    ): java.nio.file.Path {
        // 1. 读取模板序列文件（CSV 格式）
        logger.info("正在读取模板序列文件...")
        val thrSequences = readCsvSequences(templateFilePath)
        logger.info("读取到 ${thrSequences.size} 个模板序列。")

        // 2. 读取测试序列并添加 CCA
        logger.info("正在读取测试序列文件...")
        val testSequences = readFastaSequencesWithCCA(testFilePath)
        logger.info("读取到 ${testSequences.size} 个测试序列。")

        // 3. 对模板序列进行多序列比对，找出保守位点
        logger.info("正在对模板序列进行多序列比对...")
        val (alignedThrSequences, conservedPositions) = performMultipleSequenceAlignment(thrSequences)
        logger.info("模板序列多序列比对完成。")

        // 4. 对每个测试序列进行比对和打分
        logger.info("正在对测试序列进行比对和打分...")
        val results = scoreTestSequences(alignedThrSequences, conservedPositions, testSequences)
        logger.info("测试序列比对和打分完成。")

        // 5. 保存结果到 CSV 文件并排序
        logger.info("正在保存结果到 CSV 文件...")
        val resultsCsvPath = Paths.get(tempDirPath, "results.csv")
        saveResultsToCsv(results, resultsCsvPath.toString())
        logger.info("结果保存完成。")

        return resultsCsvPath
    }

    // 读取 CSV 文件中的序列
    private fun readCsvSequences(filePath: String): List<Pair<String, RNASequence>> {
        val sequences = mutableListOf<Pair<String, RNASequence>>()
        Files.newBufferedReader(Paths.get(filePath)).use { reader ->
            reader.readLine() // 跳过表头
            var index = 0
            reader.lineSequence().forEach { line ->
                val columns = line.split(",")
                if (columns.isNotEmpty()) {
                    val seqStr = columns.last().trim() // 假设序列在最后一列
                    val seq = RNASequence(seqStr)
                    val header = "Thr_seq_$index"
                    sequences.add(Pair(header, seq))
                    index++
                }
            }
        }
        return sequences
    }

    // 读取 FASTA 文件中的序列并添加 CCA
    private fun readFastaSequencesWithCCA(filePath: String): List<Pair<String, RNASequence>> {
        val sequences = mutableListOf<Pair<String, RNASequence>>()
        var header: String? = null
        val seqBuilder = StringBuilder()
        Files.newBufferedReader(Paths.get(filePath)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith(">")) {
                    if (header != null && seqBuilder.isNotEmpty()) {
                        val sequence = seqBuilder.toString() + "CCA"
                        val rnaSeq = RNASequence(sequence)
                        sequences.add(Pair(header!!, rnaSeq))
                        seqBuilder.clear()
                    }
                    header = line.trim().removePrefix(">")
                } else {
                    seqBuilder.append(line.trim())
                }
            }
            // 添加最后一个序列
            if (header != null && seqBuilder.isNotEmpty()) {
                val sequence = seqBuilder.toString() + "CCA"
                val rnaSeq = RNASequence(sequence)
                sequences.add(Pair(header!!, rnaSeq))
            }
        }
        return sequences
    }

    // 创建适用于 RNA 的替换矩阵
    private fun getRnaSubstitutionMatrix(): SubstitutionMatrix<NucleotideCompound> {
        val rnaCompounds = RNACompoundSet.getRNACompoundSet()

        // 定义匹配和不匹配的分数
        val matchScore: Short = 1
        val mismatchScore: Short = -1

        // 创建替换矩阵
        return SimpleSubstitutionMatrix(rnaCompounds, matchScore, mismatchScore)
    }

    // 使用 BioJava 进行多序列比对，返回比对后的序列和保守位点列表
    private fun performMultipleSequenceAlignment(
        sequences: List<Pair<String, RNASequence>>
    ): Pair<Map<String, String>, Set<Int>> {
        val rnaSequences = sequences.map { (header, seq) ->
            seq.accession = AccessionID(header)
            seq
        }

        val gapPenalty = SimpleGapPenalty(5, 2)

        // 使用自定义的 RNA 替换矩阵
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

        // 找出保守位点
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

    // 比对测试序列并计算得分
    private fun scoreTestSequences(
        alignedThrSequences: Map<String, String>,
        conservedPositions: Set<Int>,
        testSequences: List<Pair<String, RNASequence>>
    ): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val gapPenalty = -2

        // 将已对齐的模板序列转换为 RNASequence 对象
        val thrSeqs = alignedThrSequences.map { (header, seqStr) ->
            val seqWithoutGaps = seqStr.replace('-', 'N') // 替换缺口为 'N'
            val rnaSeq = RNASequence(seqWithoutGaps)
            rnaSeq.accession = AccessionID(header)
            rnaSeq
        }

        for ((testHeader, testSeq) in testSequences) {
            try {
                testSeq.accession = AccessionID(testHeader)

                val combinedSequences = mutableListOf<RNASequence>()
                combinedSequences.addAll(thrSeqs)
                combinedSequences.add(testSeq)

                val gapPenaltyObj = SimpleGapPenalty(5, 2)

                // 使用自定义的 RNA 替换矩阵
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
                    logger.error("未能在比对结果中找到测试序列：$testHeader")
                    continue
                }

                // 计算得分
                var score = 0.0 // 修改为 Double 类型
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

                // 归一化得分
                val normalizedScore = score / conservedPositions.size

                results.add(
                    mapOf(
                        "Test_Sequence" to testHeader,
                        "Score" to normalizedScore,
                        "Sequence" to testSeq.sequenceAsString
                    )
                )
            } catch (e: Exception) {
                logger.error("比对失败：${e.message}", e)
                continue
            }
        }

        return results
    }

    // 保存结果到 CSV 文件
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
