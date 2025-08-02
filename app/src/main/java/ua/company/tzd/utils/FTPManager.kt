package ua.company.tzd.utils

import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Допоміжний клас для роботи з FTP-сервером.
 * Дозволяє читати та записувати XML-файли з каталогів /import та /processing.
 */
class FTPManager(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val pass: String
) {
    /**
     * Створюємо підключення до сервера і повертаємо готовий клієнт.
     */
    private fun connect(): FTPClient {
        val client = FTPClient()
        client.connect(host, port)
        client.login(user, pass)
        client.enterLocalPassiveMode()
        return client
    }

    /**
     * Зчитує всі XML-файли з вказаного каталогу.
     * Повертає карту: ім'я файлу -> вміст у вигляді потоку.
     */
    fun readXmlFiles(directory: String): Map<String, InputStream> {
        val result = mutableMapOf<String, InputStream>()
        val client = connect()
        try {
            client.changeWorkingDirectory(directory)
            client.listFiles().forEach { file ->
                if (file.name.endsWith(".xml")) {
                    val stream = client.retrieveFileStream(file.name)
                    if (stream != null) {
                        // Копіюємо вміст, щоб можна було закрити FTP-з'єднання
                        val bytes = stream.readBytes()
                        stream.close()
                        result[file.name] = ByteArrayInputStream(bytes)
                        client.completePendingCommand()
                    }
                }
            }
        } finally {
            client.logout()
            client.disconnect()
        }
        return result
    }

    /**
     * Завантажує файл у заданий каталог.
     */
    fun uploadFile(directory: String, name: String, content: ByteArray) {
        val client = connect()
        try {
            client.changeWorkingDirectory(directory)
            client.storeFile(name, ByteArrayInputStream(content))
        } finally {
            client.logout()
            client.disconnect()
        }
    }
}

