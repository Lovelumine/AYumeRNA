package com.lovelumine.Rfam.utils

import java.io.*
import java.nio.file.Paths

object RfamUtils {

    @Throws(IOException::class)
    fun processRfamTask(rfamAcc: String, originalFilePath: String, pathToRfamSeed: String): String {
        // 1. 去重操作
        val uniquedFastaPath = uniquenize(originalFilePath)

        // 2. 提取种子序列并移除种子序列
        val ungappedSeedPath = fetchSeedSequence(pathToRfamSeed, rfamAcc)
        return removeSeedFromFull(ungappedSeedPath, uniquedFastaPath)
    }

    /**
     * 去除重复的序列，并生成一个新的去重后的FASTA文件
     */
    @Throws(IOException::class)
    fun uniquenize(fastaPath: String): String {
        val sequences = mutableSetOf<String>()
        val outputPath = fastaPath.replace(".fa", "_unique.fa")

        BufferedReader(FileReader(fastaPath)).use { reader ->
            BufferedWriter(FileWriter(outputPath)).use { writer ->
                var header: String? = null
                val seqBuilder = StringBuilder()

                reader.forEachLine { line ->
                    if (line.startsWith(">")) {
                        if (header != null && seqBuilder.isNotEmpty()) {
                            val sequence = seqBuilder.toString()
                            if (sequences.add(sequence)) {
                                writer.write("$header\n")
                                writer.write("$sequence\n")
                            }
                            seqBuilder.clear()
                        }
                        header = line // 记录header行
                    } else {
                        seqBuilder.append(line.trim()) // 累积序列
                    }
                }
                // 处理最后一个序列
                if (header != null && seqBuilder.isNotEmpty()) {
                    val sequence = seqBuilder.toString()
                    if (sequences.add(sequence)) {
                        writer.write("$header\n")
                        writer.write("$sequence\n")
                    }
                }
            }
        }
        return outputPath
    }

    /**
     * 提取 Rfam 种子序列，去掉空位并生成无空隙的种子序列文件
     */
    @Throws(IOException::class)
    fun fetchSeedSequence(pathToRfamSeed: String, rfamAcc: String): String {
        val outputFilePath = pathToRfamSeed.replace(".seed", "_ungapped_seed.fa")

        BufferedReader(FileReader(pathToRfamSeed)).use { reader ->
            BufferedWriter(FileWriter(outputFilePath)).use { writer ->
                var inTargetBlock = false
                val names = mutableListOf<String>()
                val sequences = mutableListOf<String>()

                reader.forEachLine { line ->
                    when {
                        line.startsWith("#=GF AC") -> {
                            val id = line.split("\\s+".toRegex()).last().trim()
                            inTargetBlock = id == rfamAcc
                        }
                        inTargetBlock -> when {
                            line.startsWith("//") -> inTargetBlock = false
                            line.startsWith(" ") -> {
                                sequences[sequences.size - 1] += line.trim().replace(" ", "")
                            }
                            else -> {
                                val parts = line.trim().split("\\s+".toRegex())
                                if (parts.size >= 2) {
                                    names.add(parts[0])
                                    sequences.add(parts[1])
                                }
                            }
                        }
                    }
                }

                // 去掉空位并生成无空隙的种子序列
                for (i in names.indices) {
                    val sequence = sequences[i].replace("-", "")
                    writer.write(">${names[i]}\n$sequence\n")
                }
            }
        }
        return outputFilePath
    }

    /**
     * 从全序列文件中移除与种子序列相同的序列，并生成一个去除后的FASTA文件
     */
    @Throws(IOException::class)
    fun removeSeedFromFull(fastaSeed: String, fastaFull: String): String {
        val seedSequences = mutableSetOf<String>()

        // 读取种子序列并存储到集合中
        BufferedReader(FileReader(fastaSeed)).use { reader ->
            var seqBuilder = StringBuilder()
            reader.forEachLine { line ->
                if (line.startsWith(">")) {
                    if (seqBuilder.isNotEmpty()) {
                        seedSequences.add(seqBuilder.toString())
                        seqBuilder = StringBuilder()
                    }
                } else {
                    seqBuilder.append(line.trim().toUpperCase().replace("T", "U"))
                }
            }
            // 添加最后一个序列
            if (seqBuilder.isNotEmpty()) {
                seedSequences.add(seqBuilder.toString())
            }
        }

        // 从全序列中移除种子序列，并生成去除后的FASTA文件
        val outputPath = fastaFull.replace(".fa", "_seed_removed.fa")
        BufferedReader(FileReader(fastaFull)).use { reader ->
            BufferedWriter(FileWriter(outputPath)).use { writer ->
                var header: String? = null
                var seqBuilder = StringBuilder()

                reader.forEachLine { line ->
                    if (line.startsWith(">")) {
                        if (header != null && seqBuilder.isNotEmpty()) {
                            val sequence = seqBuilder.toString()
                            if (sequence !in seedSequences && sequence.matches(Regex("[ACGU]+"))) {
                                writer.write("$header\n$sequence\n")
                            }
                            seqBuilder = StringBuilder()
                        }
                        header = line
                    } else {
                        seqBuilder.append(line.trim().toUpperCase().replace("T", "U"))
                    }
                }
                // 写入最后一个序列
                if (header != null && seqBuilder.isNotEmpty()) {
                    val sequence = seqBuilder.toString()
                    if (sequence !in seedSequences && sequence.matches(Regex("[ACGU]+"))) {
                        writer.write("$header\n$sequence\n")
                    }
                }
            }
        }

        return outputPath
    }
}
