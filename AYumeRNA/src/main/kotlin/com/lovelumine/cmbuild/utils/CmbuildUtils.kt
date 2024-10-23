package com.lovelumine.cmbuild.utils

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

object CmbuildUtils {

    private val logger = LoggerFactory.getLogger(CmbuildUtils::class.java)

    fun runCmbuild(stockholmFilePath: String, onDetailsUpdated: (String) -> Unit): String {
        // 转换路径为 WSL 格式
        val wslStockholmPath = convertToWslPath(stockholmFilePath)
        val outputCmFilePath = stockholmFilePath.replace(".stockholm", ".cm")
        val wslCmFilePath = convertToWslPath(outputCmFilePath)

        // 生成 WSL 下可用的命令
        val command = "wsl cmbuild $wslCmFilePath $wslStockholmPath"
        logger.info("执行 Cmbuild 命令：$command")
        executeCommand(command, onDetailsUpdated)
        return outputCmFilePath
    }

    private fun convertToWslPath(windowsPath: String): String {
        return windowsPath.replace("C:\\", "/mnt/c/")
            .replace("\\", "/")
            .replace(" ", "\\ ")
    }

    private fun executeCommand(command: String, onDetailsUpdated: (String) -> Unit) {
        val processBuilder = ProcessBuilder(command.split(" "))
        processBuilder.redirectErrorStream(true)
        logger.info("启动进程：$command")
        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            logger.info("命令输出：$line")
            onDetailsUpdated(line ?: "")
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            logger.error("命令执行失败，退出码：$exitCode")
            throw RuntimeException("Failed to execute command: $command, exit code: $exitCode")
        }
        logger.info("命令执行成功，退出码：$exitCode")
    }
}
