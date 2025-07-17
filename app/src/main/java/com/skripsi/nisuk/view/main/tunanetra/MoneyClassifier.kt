package com.skripsi.nisuk.view.main.tunanetra

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

class MoneyClassifier(private val context: Context) {

    private val interpreter: Interpreter

    init {
        val model = loadModelFile()
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("model1.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    fun classifyImage(image: Bitmap): Int {
        val inputBuffer = convertBitmapToByteBuffer(image)
        val output = Array(1) { FloatArray(7) } // 7 = jumlah kelas
        interpreter.run(inputBuffer, output)

        // Ambil indeks dengan nilai probabilitas tertinggi
        return output[0].indices.maxByOrNull { output[0][it] } ?: -1
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 200
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val intValues = IntArray(inputSize * inputSize)
        resized.getPixels(intValues, 0, resized.width, 0, 0, resized.width, resized.height)

        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }
}
