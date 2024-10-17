package com.lovelumine.tREX.service

import org.biojava.nbio.alignment.Alignments
import org.biojava.nbio.core.alignment.template.Profile
import org.biojava.nbio.core.alignment.template.AlignedSequence
import org.biojava.nbio.core.sequence.RNASequence
import org.biojava.nbio.core.sequence.compound.NucleotideCompound
import org.biojava.nbio.core.sequence.io.FastaReaderHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream

@Service
class SequenceService {

    // 从字符串内容解析序列
    fun parseSequences(content: String): List<RNASequence> {
        // 将字符串转换为 InputStream
        val inputStream: InputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

        // 使用 InputStream 解析 FASTA 格式的字符串
        val sequences = FastaReaderHelper.readFastaRNASequence(inputStream)
        return sequences.values.toList()
    }

    // 从字符串内容解析序列并添加 CCA
    fun parseSequencesWithCCA(content: String): List<Pair<String, RNASequence>> {
        val inputStream: InputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        val sequences = FastaReaderHelper.readFastaRNASequence(inputStream)
        return sequences.map { entry ->
            val seqWithCCA = RNASequence(entry.value.sequenceAsString + "CCA")
            Pair(entry.key, seqWithCCA)
        }
    }

    // 进行多序列比对
    fun alignSequences(sequences: List<RNASequence>): Profile<RNASequence, NucleotideCompound> {
        if (sequences.size < 2) {
            throw IllegalArgumentException("序列数量不足，无法进行多序列比对。")
        }
        return Alignments.getMultipleSequenceAlignment(sequences)
    }

    // 计算测试序列的得分
    fun calculateScores(
        templateSequences: List<RNASequence>,
        testSequences: List<Pair<String, RNASequence>>
    ): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()

        for ((name, testSeq) in testSequences) {
            // 将模板序列和测试序列组合
            val combinedSequences = templateSequences.toMutableList()
            combinedSequences.add(testSeq)
            // 进行多序列比对
            val profile = alignSequences(combinedSequences)
            // 计算得分
            val score = computeScore(profile, templateSequences.size)
            results.add(
                mapOf(
                    "Test_Sequence" to name,
                    "Score" to score,
                    "Sequence" to testSeq.sequenceAsString
                )
            )
        }
        return results
    }

    // 计算得分的辅助函数
    private fun computeScore(
        profile: Profile<RNASequence, NucleotideCompound>,
        templateSeqCount: Int
    ): Double {
        var score = 0
        val alignedSequences = profile.alignedSequences
        val length = alignedSequences[0].length
        val testSeq = alignedSequences.last() as AlignedSequence<RNASequence, NucleotideCompound>

        for (i in 1..length) {
            val testBase = testSeq.getCompoundAt(i)
            for (j in 0 until templateSeqCount) {
                val templateSeq = alignedSequences[j] as AlignedSequence<RNASequence, NucleotideCompound>
                val templateBase = templateSeq.getCompoundAt(i)
                if (testBase == templateBase) {
                    score += 1
                } else {
                    score -= 1
                }
            }
        }
        // 计算平均得分
        val avgScore = score.toDouble() / templateSeqCount
        return avgScore
    }
}
