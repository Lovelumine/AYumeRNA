package com.lovelumine.Rfam.utils

import java.io.*
import java.nio.file.Paths

object RfamUtils {

    @Throws(IOException::class)
    fun processRfamTask(
        rfamAcc: String,
        originalFilePath: String,
        pathToRfamSeed: String,
        onDetailsUpdated: (foundSeq: Int, seedSeqCount: Int, retainedSeqCount: Int) -> Unit
    ): String {

        // 1. 去重操作
        val uniquedFastaPath = uniquenize(originalFilePath)

        // 2. 提取种子序列并移除种子序列
        val ungappedSeedPath = fetchSeedSequence(pathToRfamSeed, rfamAcc)
        val resultPath = removeSeedFromFull(ungappedSeedPath, uniquedFastaPath, onDetailsUpdated)

        return resultPath
    }

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

    @Throws(IOException::class)
    fun fetchSeedSequence(pathToRfamSeed: String, rfamAcc: String): String {
        val outputFilePath = pathToRfamSeed.replace(".seed", "_ungapped_seed.fa")

        BufferedReader(FileReader(pathToRfamSeed)).use { reader ->
            BufferedWriter(FileWriter(outputFilePath)).use { writer ->
                var inTargetBlock = false
                val names = mutableSetOf<String>()  // 改为集合，确保不重复计数
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
                            line.startsWith(">") -> {
                                // 统计新的种子序列头
                                val parts = line.trim().split("\\s+".toRegex())
                                names.add(parts[0])  // 使用集合来防止重复
                                sequences.add("")
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
                    writer.write(">${names.elementAt(i)}\n$sequence\n")
                }
            }
        }
        return outputFilePath
    }

    @Throws(IOException::class)
    fun removeSeedFromFull(
        fastaSeed: String,
        fastaFull: String,
        onDetailsUpdated: (foundSeq: Int, seedSeqCount: Int, retainedSeqCount: Int) -> Unit
    ): String {
        val seedSequences = mutableSetOf<String>()
        var foundSeq = 0
        var seedSeqCount = 0
        var retainedSeqCount = 0

        // 读取种子序列
        BufferedReader(FileReader(fastaSeed)).use { reader ->
            var seqBuilder = StringBuilder()
            reader.forEachLine { line ->
                if (line.startsWith(">")) {
                    if (seqBuilder.isNotEmpty()) {
                        seedSequences.add(seqBuilder.toString())
                        seqBuilder = StringBuilder()
                        seedSeqCount++
                    }
                } else {
                    seqBuilder.append(line.trim().toUpperCase().replace("T", "U"))
                }
            }
            if (seqBuilder.isNotEmpty()) {
                seedSequences.add(seqBuilder.toString())
                seedSeqCount++
            }
        }

        // 读取全序列并移除种子序列
        val outputPath = fastaFull.replace(".fa", "_seed_removed.fa")
        BufferedReader(FileReader(fastaFull)).use { reader ->
            BufferedWriter(FileWriter(outputPath)).use { writer ->
                var header: String? = null
                var seqBuilder = StringBuilder()

                reader.forEachLine { line ->
                    if (line.startsWith(">")) {
                        if (header != null && seqBuilder.isNotEmpty()) {
                            val sequence = seqBuilder.toString()
                            foundSeq++
                            if (sequence !in seedSequences && sequence.matches(Regex("[ACGU]+"))) {
                                writer.write("$header\n$sequence\n")
                                retainedSeqCount++
                            }
                            seqBuilder = StringBuilder()
                        }
                        header = line
                    } else {
                        seqBuilder.append(line.trim().toUpperCase().replace("T", "U"))
                    }
                }
                if (header != null && seqBuilder.isNotEmpty()) {
                    foundSeq++
                    val sequence = seqBuilder.toString()
                    if (sequence !in seedSequences && sequence.matches(Regex("[ACGU]+"))) {
                        writer.write("$header\n$sequence\n")
                        retainedSeqCount++
                    }
                }
            }
        }

        // 回调进度信息
        onDetailsUpdated(foundSeq, seedSeqCount, retainedSeqCount)

        return outputPath
    }
}
