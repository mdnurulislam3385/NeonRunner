package com.kadeza.neonrunner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import kotlin.math.max
import kotlin.random.Random

class RunnerView(context: Context, private val action: (String, Int) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = context.getSharedPreferences("neon_runner", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var screen = 0 // 0 menu, 1 playing, 2 game over, 3 store
    private var playerLane = 1
    private var score = 0
    private var coins = prefs.getInt("coins", 0)
    private var best = prefs.getInt("best", 0)
    private var speed = 6f
    private var lastTime = System.currentTimeMillis()
    private var spawn = 0f
    private var jump = 0f
    private var items = mutableListOf<Item>()
    private var downX = 0f
    private var downY = 0f
    private var paused = false
    private data class Item(var lane: Int, var y: Float, var coin: Boolean, var size: Float = 1f)

    private val tick = object : Runnable {
        override fun run() {
            if (screen == 1 && !paused) update()
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null); handler.post(tick) }
    fun showMenu() { screen = 0; paused = false; invalidate() }
    fun addCoins(n: Int) { coins += n; save(); invalidate() }
    private fun save() { prefs.edit().putInt("coins", coins).putInt("best", best).apply() }

    private fun start() { screen = 1; score = 0; speed = 6f; spawn = 0f; jump = 0f; playerLane = 1; items.clear(); paused = false; lastTime = System.currentTimeMillis() }
    private fun update() {
        val now = System.currentTimeMillis(); val dt = ((now - lastTime).coerceAtMost(50)) / 16f; lastTime = now
        speed = minOf(19f, speed + 0.0025f * dt)
        score += (speed * 0.04f * dt).toInt().coerceAtLeast(0)
        spawn += dt
        if (spawn > max(18f, 55f - speed * 1.7f)) {
            spawn = 0f
            items.add(Item(Random.nextInt(3), -60f, coin = Random.nextFloat() < 0.62f))
        }
        val h = height.toFloat()
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next(); item.y += speed * dt * 2.2f
            val playerY = h * 0.79f
            if (item.y > playerY - 34 && item.y < playerY + 48 && item.lane == playerLane) {
                if (item.coin) { coins += 1; save(); iterator.remove() }
                else if (jump <= 0f) { endGame(); return }
            }
            if (item.y > h + 70) iterator.remove()
        }
        if (jump > 0f) jump -= dt
    }
    private fun endGame() { screen = 2; best = max(best, score); save() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(0xFF06162D.toInt())
        // sky gradient-like bands
        p.color = 0xFF0877B9.toInt(); c.drawRect(0f, 0f, w, h * .55f, p)
        p.color = 0xFF12B9D6.toInt(); c.drawRect(0f, h * .55f, w, h, p)
        p.color = 0xFF0A3568.toInt(); c.drawRoundRect(RectF(w*.06f,h*.08f,w*.94f,h*.96f), 34f,34f,p)
        p.color = 0xFF14558A.toInt(); c.drawRoundRect(RectF(w*.09f,h*.10f,w*.91f,h*.94f), 28f,28f,p)
        // track
        p.color = 0xFF082743.toInt(); c.drawRoundRect(RectF(w*.16f,h*.14f,w*.84f,h*.91f), 28f,28f,p)
        p.color = 0xFF1E5B80.toInt()
        for (i in 1..2) c.drawRect(w*(.16f + .68f*i/3f)-2,w*.0f+h*.15f,w*(.16f+.68f*i/3f)+2,h*.90f,p)
        // decorative stars
        p.color = 0xFF9BEAFF.toInt()
        for (i in 0..12) { val x = (w * ((i * 37 % 97) / 100f)); val y = (h * ((i * 23 % 87) / 100f)); c.drawCircle(x,y,2.3f,p) }

        if (screen == 0) {
            text(c,"NEON RUNNER",w/2,h*.23f,38f,0xFFFFFFFF.toInt(),true)
            text(c,"DODGE • COLLECT • SURVIVE",w/2,h*.29f,13f,0xFFB7F6FF.toInt())
            drawRunner(c,w/2,h*.48f,1f)
            button(c,"PLAY",w/2,h*.65f,w*.60f,0xFF16D9C5.toInt())
            button(c,"COIN STORE",w/2,h*.74f,w*.60f,0xFFFFC857.toInt())
            text(c,"BEST  $best",w/2,h*.84f,17f,0xFFFFFFFF.toInt(),true)
            hud(c,w)
        } else if (screen == 3) {
            text(c,"COIN STORE",w/2,h*.16f,30f,0xFFFFFFFF.toInt(),true)
            text(c,"Your coins: $coins",w/2,h*.22f,18f,0xFFFFD85B.toInt(),true)
            val labels = listOf("500 COINS", "1,500 COINS", "5,000 COINS", "12,000 COINS")
            for (i in 0..3) {
                val yy = h*(.34f + i*.12f)
                p.color = 0xFF176DA5.toInt(); c.drawRoundRect(RectF(w*.16f,yy-31,w*.84f,yy+31),18f,18f,p)
                text(c,labels[i],w*.38f,yy+6,18f,0xFFFFFFFF.toInt(),true)
                p.color = 0xFF28C76F.toInt(); c.drawRoundRect(RectF(w*.61f,yy-21,w*.80f,yy+21),16f,16f,p)
                text(c,"BUY",w*.705f,yy+6,13f,0xFFFFFFFF.toInt(),true)
            }
            text(c,"Prices are loaded from Google Play",w/2,h*.86f,12f,0xFFB8E9FF.toInt())
            button(c,"BACK",w/2,h*.92f,w*.48f,0xFF365B7B.toInt())
        } else if (screen == 1) {
            hud(c,w)
            text(c,"SPEED  ${"%.1f".format(speed)}x",w/2,h*.19f,14f,0xFFFFFFFF.toInt(),true)
            items.forEach { item ->
                val x = laneX(w,item.lane)
                if (item.coin) {
                    p.color = 0xFFFFC928.toInt(); c.drawCircle(x,item.y,15f,p)
                    p.color = 0xFFFFF2A0.toInt(); c.drawCircle(x,item.y,8f,p)
                } else {
                    p.color = 0xFFFF496C.toInt(); c.drawRoundRect(RectF(x-19,item.y-20,x+19,item.y+20),8f,8f,p)
                    p.color = 0xFFFFB6C5.toInt(); c.drawRect(x-10,item.y-11,x+10,item.y-5,p)
                }
            }
            val py = h*.79f - if (jump>0) 70f else 0f
            drawRunner(c,laneX(w,playerLane),py,1f)
            button(c,if(paused) "RESUME" else "Ⅱ",w*.83f,h*.13f,w*.22f,0xFF173F64.toInt())
        } else {
            text(c,"RUN OVER",w/2,h*.28f,34f,0xFFFFFFFF.toInt(),true)
            text(c,"SCORE  $score",w/2,h*.39f,24f,0xFFFFD65A.toInt(),true)
            text(c,"BEST  $best",w/2,h*.45f,17f,0xFFB9F8FF.toInt())
            button(c,"PLAY AGAIN",w/2,h*.59f,w*.62f,0xFF16D9C5.toInt())
            button(c,"COIN STORE",w/2,h*.69f,w*.62f,0xFFFFC857.toInt())
            button(c,"HOME",w/2,h*.79f,w*.62f,0xFF365B7B.toInt())
            hud(c,w)
        }
    }

    private fun laneX(w: Float, lane: Int) = w*(.16f + .68f*(lane+.5f)/3f)
    private fun drawRunner(c: Canvas,x:Float,y:Float,s:Float) {
        p.color=0xFF152E50.toInt(); c.drawOval(RectF(x-26*s,y+25*s,x+26*s,y+35*s),p)
        p.color=0xFF16D9C5.toInt(); c.drawRoundRect(RectF(x-17*s,y-25*s,x+17*s,y+20*s),12*s,12*s,p)
        p.color=0xFFFFD3A6.toInt(); c.drawCircle(x,y-34*s,13*s,p)
        p.color=0xFF112C4A.toInt(); c.drawRoundRect(RectF(x-13*s,y-45*s,x+13*s,y-35*s),7*s,7*s,p)
        p.color=0xFFFFFFFF.toInt(); c.drawCircle(x-4*s,y-35*s,2.2f*s,p); c.drawCircle(x+4*s,y-35*s,2.2f*s,p)
        p.color=0xFF9B7BFF.toInt(); c.drawRoundRect(RectF(x-20*s,y+13*s,x-5*s,y+32*s),6*s,6*s,p); c.drawRoundRect(RectF(x+5*s,y+13*s,x+20*s,y+32*s),6*s,6*s,p)
    }
    private fun hud(c:Canvas,w:Float) {
        p.color=0xCC08233E.toInt(); c.drawRoundRect(RectF(w*.12f,height*.035f,w*.88f,height*.095f),22f,22f,p)
        text(c,"♥ ♥ ♥",w*.30f,height*.072f,15f,0xFFFF5277.toInt(),true)
        text(c,"🪙 $coins",w*.70f,height*.072f,15f,0xFFFFD65A.toInt(),true)
    }
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false) { p.color=color; p.textSize=size; p.textAlign=Paint.Align.CENTER; p.typeface=if(bold) android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD) else android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL); c.drawText(s,x,y,p) }
    private fun button(c:Canvas,label:String,cx:Float,cy:Float,bw:Float,color:Int) { p.color=color; c.drawRoundRect(RectF(cx-bw/2,cy-25,cx+bw/2,cy+25),22f,22f,p); text(c,label,cx,cy+6,17f,0xFF06233D.toInt(),true) }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) { downX=e.x; downY=e.y; return true }
        if (e.action == MotionEvent.ACTION_UP) {
            val dx=e.x-downX; val dy=e.y-downY; val w=width.toFloat(); val h=height.toFloat()
            if (screen==1) {
                if (downY < h*.18f && downX > w*.68f) { paused=!paused; return true }
                if (kotlin.math.abs(dx)>45) playerLane=(playerLane + if(dx>0) 1 else -1).coerceIn(0,2)
                else if (dy < -35) jump=14f
                else if (dy > 35) playerLane=(playerLane+1).coerceAtMost(2)
                else if (downX<w*.5f) playerLane=(playerLane-1).coerceAtLeast(0) else playerLane=(playerLane+1).coerceAtMost(2)
            } else if (screen==0) {
                if (downY > h*.59f && downY < h*.70f) start()
                else if (downY > h*.70f && downY < h*.80f) screen=3
            } else if (screen==3) {
                if (downY > h*.29f && downY < h*.78f) { val i=((downY/h-.34f)/.12f).toInt().coerceIn(0,3); action("buy",i) }
                else if (downY > h*.87f) screen=0
            } else if (screen==2) {
                when { downY > h*.53f && downY < h*.64f -> start(); downY in (h*.64f)..(h*.75f) -> screen=3; downY > h*.75f -> screen=0 }
            }
            invalidate(); return true
        }
        return true
    }
}
