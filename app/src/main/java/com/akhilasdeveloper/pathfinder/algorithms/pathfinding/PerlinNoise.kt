package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import kotlin.math.cos
import kotlin.math.floor

class PerlinNoise() {

    val PERLIN_YWRAPB = 4
    val PERLIN_YWRAP = 1 shl PERLIN_YWRAPB
    val PERLIN_ZWRAPB = 8
    val PERLIN_ZWRAP = 1 shl PERLIN_ZWRAPB
    val PERLIN_SIZE = 4095

    var perlin_octaves = 4 // default to medium smooth
    var perlin_amp_falloff = 0.5 // 50% reduction/octave

    val scaled_cosine: (i: Int) -> Double = {i-> 0.5 * (1.0 - cos(i * Math.PI)) }

    var perlin:DoubleArray? = null // will be initialized lazily by noise() or noiseSeed()

    fun noise(xx:Float, yy:Float = 0f, zz:Float = 0f):Double
    {
        var x = xx
        var y = yy
        var z = zz

        if (perlin == null) {
            perlin = DoubleArray(PERLIN_SIZE + 1)
            for (i in 0 until PERLIN_SIZE + 1) {
                perlin!![i] = Math.random()
            }
        }

        if (x < 0) {
            x = -x;
        }
        if (y < 0) {
            y = -y;
        }
        if (z < 0) {
            z = -z;
        }

        var xi = floor(x).toInt()
        var yi = floor(y).toInt()
        var zi = floor(z).toInt()
        var xf = (x - xi).toInt()
        var yf = (y - yi).toInt()
        var zf = (z - zi).toInt()
        var rxf = 0.0
        var ryf = 0.0

        var r = 0.0
        var ampl = 0.5

        var n1 = 0.0
        var n2 = 0.0
        var n3 = 0.0

        for (o in 0 until perlin_octaves) {
        var of = xi +(yi shl PERLIN_YWRAPB) + (zi shl PERLIN_ZWRAPB);

        rxf = scaled_cosine(xf)
        ryf = scaled_cosine(yf)

        n1 = perlin!![of and PERLIN_SIZE];
        n1 += rxf * (perlin!![(of + 1) and PERLIN_SIZE]-n1)
        n2 = perlin!![(of + PERLIN_YWRAP) and PERLIN_SIZE]
        n2 += rxf * (perlin!![(of + PERLIN_YWRAP + 1) and PERLIN_SIZE]-n2)
        n1 += ryf * (n2 - n1)

        of += PERLIN_ZWRAP;
        n2 = perlin!![of and PERLIN_SIZE];
        n2 += rxf * (perlin!![(of + 1) and PERLIN_SIZE]-n2)
        n3 = perlin!![(of + PERLIN_YWRAP) and PERLIN_SIZE]
        n3 += rxf * (perlin!![(of + PERLIN_YWRAP + 1) and PERLIN_SIZE]-n3)
        n2 += ryf * (n3 - n2);

        n1 += scaled_cosine(zf) * (n2 - n1);

        r += n1 * ampl
        ampl *= perlin_amp_falloff;
        xi  = xi shl 1
        xf *= 2
        yi = yi shl 1
        yf *= 2
        zi = zi shl 1
        zf *= 2

        if (xf >= 1.0) {
            xi++;
            xf--;
        }
        if (yf >= 1.0) {
            yi++;
            yf--;
        }
        if (zf >= 1.0) {
            zi++;
            zf--;
        }
    }
        return r
    }

}