import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.widget.Toast
import com.akhilasdeveloper.pathfinder.models.Node
import java.io.IOException
import java.io.InputStream
import kotlin.math.sqrt


class GetPixels(val context: Context) {

    private val scale = 10
    private val margin = 2
    private var width = 0
    private var heights = 0

    fun getPixelsPos(viewPortWidth: Int, viewPortHeight: Int): Bitmap? {
        width = (viewPortWidth - (2* margin)) / (margin + scale)
        heights = (viewPortHeight - (2* margin)) / (margin + scale)
        var bitmap: Bitmap? = null
        getImg()?.let { bmp ->

            val height = ((bmp.height.toFloat()/bmp.width.toFloat()) * width).toInt()
            val img = Bitmap.createScaledBitmap(bmp,width,height,false)

            Toast.makeText(context,"$width , $height", Toast.LENGTH_SHORT).show()

            val bwBitmap = Bitmap.createBitmap(viewPortWidth, viewPortHeight, Bitmap.Config.RGB_565)
            bwBitmap.eraseColor(Color.WHITE)
            val hsv = FloatArray(3)
            val l = mutableListOf<Node>()

            var yy = margin
            for (y in 0 until heights) {
                var xx = margin
                for (x in 0 until width) {
                    if (y < height) {
                        Color.colorToHSV(img.getPixel(x, y), hsv)
                        if (hsv[2] <= 0.5f) {
                            l.add(
                                Node(
//                                    isSelected = true,
                                    x = x,
                                    y = y
                                )
                            )
                            for (i in xx..(xx + scale)) {
                                for (j in yy..(yy + scale)) {
                                    bwBitmap.setPixel(i, j, 0xff000000.toInt())
                                }
                            }
                        } else {
                            for (i in xx..(xx + scale)) {
                                for (j in yy..(yy + scale)) {
                                    bwBitmap.setPixel(i, j, 0xffbbbbbb.toInt())
                                }
                            }
                        }
                    }else {
                        for (i in xx..(xx + scale)) {
                            for (j in yy..(yy + scale)) {
                                bwBitmap.setPixel(i, j, 0xffbbbbbb.toInt())
                            }
                        }
                    }
                    xx += scale + margin
                }
                yy += scale + margin
            }
            bitmap = bwBitmap
        }
        return bitmap
    }

    private fun getImg(): Bitmap? {
        val assetManager = context.assets

        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open("down.jpg")
            bitmap = BitmapFactory.decodeStream(istr)
            bitmap?.let {

            }
        } catch (e: IOException) {
            // handle exception
        }

        return bitmap?.rotate(90f)
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}