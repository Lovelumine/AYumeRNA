package com.lovelumine.tREX.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class SequenceService {

    private val logger = LoggerFactory.getLogger(SequenceService::class.java)

    fun processSequences(
        templateFilePath: String,
        testFilePath: String,
        tempDirPath: String
    ): java.nio.file.Path {
        // 1. 读取 Thr 序列文件（CSV 格式）
        logger.info("正在读取 Thr 序列文件...")
        val thrSequences = readCsvSequences(templateFilePath)
        logger.info("读取到 ${thrSequences.size} 个 Thr 序列。")

        // 2. 读取测试序列并添加 CCA
        logger.info("正在读取测试序列文件...")
        val testSequences = readFastaSequencesWithCCA(testFilePath)
        logger.info("读取到 ${testSequences.size} 个测试序列。")

        // 3. 将 Thr 序列写入 FASTA 文件
        val thrFastaPath = Paths.get(tempDirPath, "Thr_sequences.fasta")
        val thrSequencesWithHeaders = thrSequences.mapIndexed { index, seq -> Pair("Thr_seq_$index", seq) }
        writeSequencesToFasta(thrSequencesWithHeaders, thrFastaPath.toString())
        logger.info("Thr 序列已写入 FASTA 文件。")

        // 4. 进行多序列比对
        logger.info("正在进行多序列比对...")
        val thrAlignmentPath = runClustalW(thrFastaPath.toString())
        logger.info("多序列比对完成。")

        // 5. 比对测试序列并计算得分
        logger.info("正在进行测试序列比对和打分...")
        val results = scoreTestSequences(thrSequences, testSequences, tempDirPath)
        logger.info("测试序列比对和打分完成。")

        // 6. 保存结果到 CSV 文件并排序
        logger.info("正在保存结果到 CSV 文件...")
        val resultsCsvPath = Paths.get(tempDirPath, "results.csv")
        saveResultsToCsv(results, resultsCsvPath.toString())
        logger.info("结果保存完成。")

        return resultsCsvPath
    }

    // 读取 CSV 文件中的序列
    private fun readCsvSequences(filePath: String): List<String> {
        val sequences = mutableListOf<String>()
        Files.lines(Paths.get(filePath)).use { lines ->
            lines.skip(1) // 跳过表头
                .forEach { line ->
                    val columns = line.split(",")
                    if (columns.isNotEmpty()) {
                        sequences.add(columns.last().trim()) // 假设序列在最后一列
                    }
                }
        }
        return sequences
    }

    // 读取 FASTA 文件中的序列并添加 CCA
    private fun readFastaSequencesWithCCA(filePath: String): List<Pair<String, String>> {
        val sequences = mutableListOf<Pair<String, String>>()
        var header: String? = null
        Files.lines(Paths.get(filePath)).use { lines ->
            lines.forEach { line ->
                if (line.startsWith(">")) {
                    header = line.trim()
                } else {
                    val sequence = line.trim() + "CCA"
                    sequences.add(Pair(header ?: ">Unknown", sequence))
                }
            }
        }
        return sequences
    }

    // 将序列写入 FASTA 文件
    private fun writeSequencesToFasta(
        sequences: List<Pair<String, String>>,
        filePath: String
    ) {
        val file = File(filePath)
        file.bufferedWriter().use { writer ->
            sequences.forEach { (header, seq) ->
                writer.write(">$header\n")
                writer.write("$seq\n")
            }
        }
    }

    // 调用 ClustalW 进行多序列比对
    private fun runClustalW(inputFilePath: String): String {
        val clustalwExe = "C:\\Program Files (x86)\\ClustalW2\\clustalw2.exe" // 修改为实际路径

        // 检查可执行文件是否存在
        val clustalwFile = File(clustalwExe)
        if (!clustalwFile.exists()) {
            throw FileNotFoundException("ClustalW 可执行文件未找到：$clustalwExe")
        }

        // 构建命令
        val command = listOf(clustalwExe, "-INFILE=$inputFilePath", "-OUTPUT=CLUSTAL")
        logger.info("运行 ClustalW 命令：${command.joinToString(" ")}")

        // 设置环境变量，确保 PATH 包含 Clustalw2 的路径
        val processBuilder = ProcessBuilder(command)
        val env = processBuilder.environment()
        env["PATH"] = env["PATH"] + ";${clustalwFile.parent}"

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        logger.info("ClustalW 输出：\n$output")

        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroy()
            throw RuntimeException("ClustalW 运行超时")
        }

        if (process.exitValue() != 0) {
            throw RuntimeException("ClustalW 运行失败，退出代码：${process.exitValue()}，输出：$output")
        }

        return inputFilePath.replace(".fasta", ".aln")
    }


    // 手动解析 ClustalW 比对结果
    private fun parseClustalAlignment(alignmentFilePath: String): Map<String, String> {
        val alignedSequences = mutableMapOf<String, StringBuilder>()
        val file = File(alignmentFilePath)
        if (!file.exists()) {
            throw FileNotFoundException("比对文件未找到：$alignmentFilePath")
        }

        val lines = file.readLines()
        for (line in lines) {
            if (line.isBlank() || line.startsWith("CLUSTAL") || line.startsWith("MUSCLE")) {
                // 跳过头部信息和空行
                continue
            }

            if (line.contains("*") || line.contains(":") || line.contains(".")) {
                // 跳过比对标记行
                continue
            }

            val tokens = line.trim().split(Regex("\\s+"))
            if (tokens.size >= 2) {
                val seqName = tokens[0]
                val seqData = tokens[1]

                val seqBuilder = alignedSequences.getOrPut(seqName) { StringBuilder() }
                seqBuilder.append(seqData)
            }
        }

        // 将 StringBuilder 转换为 String
        return alignedSequences.mapValues { it.value.toString() }
    }

    // 比对测试序列并计算得分
    private fun scoreTestSequences(
        thrSequences: List<String>,
        testSequences: List<Pair<String, String>>,
        tempDirPath: String
    ): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        val clustalwExe = "Clustalw2"

        for ((testName, testSeq) in testSequences) {
            var score = 0
            // 准备组合的序列列表
            val combinedSequences = mutableListOf<Pair<String, String>>()
            thrSequences.forEachIndexed { index, seq ->
                combinedSequences.add(Pair("Thr_seq_$index", seq))
            }
            combinedSequences.add(Pair(testName.removePrefix(">"), testSeq))

            // 写入组合的 FASTA 文件
            val combinedFastaPath = Paths.get(tempDirPath, "combined_sequences.fasta")
            writeSequencesToFasta(combinedSequences, combinedFastaPath.toString())

            // 进行比对
            try {
                val alignmentPath = runClustalW(combinedFastaPath.toString())
                // 手动解析比对结果
                val alignedSequences = parseClustalAlignment(alignmentPath)

                // 获取对齐的 Thr 序列和测试序列
                val thrSeqNames = thrSequences.indices.map { "Thr_seq_$it" }
                val alignedThrSeqs = thrSeqNames.mapNotNull { alignedSequences[it] }
                val alignedTestSeq = alignedSequences[testName.removePrefix(">")]

                if (alignedTestSeq == null) {
                    logger.error("未能在比对结果中找到测试序列：$testName")
                    continue
                }

                // 计算得分
                val alignmentLength = alignedTestSeq.length
                for (i in 0 until alignmentLength) {
                    val testBase = alignedTestSeq[i]
                    for (thrSeq in alignedThrSeqs) {
                        val thrBase = thrSeq[i]
                        if (testBase == thrBase) {
                            score += 1
                        } else {
                            score -= 1
                        }
                    }
                }
                val avgScore = score.toDouble() / thrSequences.size
                results.add(
                    mapOf(
                        "Test_Sequence" to testName,
                        "Score" to avgScore,
                        "Sequence" to testSeq
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
                    writer.write("${result["Test_Sequence"]},${result["Score"]},${result["Sequence"]}\n")
                }
        }
    }
}
