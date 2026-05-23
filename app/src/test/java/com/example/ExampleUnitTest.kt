package com.example

import org.junit.Assert.*
import org.junit.Test
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Local unit test to programmatically generate highly polished launcher icons
 * matching user requirements (replacing "UERJ" with "Power of Connection" branding).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun generateIcons() {
    System.setProperty("java.awt.headless", "true")
    
    val sizes = mapOf(
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192
    )
    
    val targetDirs = listOf(
        "/app/src/main/res",
        "src/main/res",
        "app/src/main/res"
    )
    
    for ((density, size) in sizes) {
        val sqImg = drawLogo(size, false)
        val rndImg = drawLogo(size, true)
        
        for (basePath in targetDirs) {
            val dir = File(basePath, "mipmap-$density")
            if (File(basePath).exists()) {
                dir.mkdirs()
                ImageIO.write(sqImg, "png", File(dir, "ic_launcher.png"))
                ImageIO.write(rndImg, "png", File(dir, "ic_launcher_round.png"))
                println("Generated icons for $density ($size px) at ${dir.absolutePath}")
            }
        }
    }
  }

  private fun drawLogo(size: Int, round: Boolean): BufferedImage {
    val baseSize = 512
    val img = BufferedImage(baseSize, baseSize, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    
    // Draw background
    if (round) {
        val clip = Ellipse2D.Float(10f, 10f, 492f, 492f)
        g.color = Color(0, 0, 0, 0)
        g.fillRect(0, 0, baseSize, baseSize)
        
        val paint = GradientPaint(0f, 0f, Color(0x1E, 0x40, 0xAF), 0f, baseSize.toFloat(), Color(0x17, 0x25, 0x54))
        g.paint = paint
        g.fill(clip)
        
        // Draw a subtle golden border for round icons
        g.color = Color(0xFA, 0xCC, 0x15, 80) // Gold 80 alpha
        g.stroke = BasicStroke(6f)
        g.draw(clip)
    } else {
        // Rounded rectangle for standard squircle launcher style
        val clip = RoundRectangle2D.Float(10f, 10f, 492f, 492f, 100f, 100f)
        g.color = Color(0, 0, 0, 0)
        g.fillRect(0, 0, baseSize, baseSize)
        
        val paint = GradientPaint(0f, 0f, Color(0x1E, 0x40, 0xAF), 0f, baseSize.toFloat(), Color(0x17, 0x25, 0x54))
        g.paint = paint
        g.fill(clip)
        
        // Subtle golden border
        g.color = Color(0xFA, 0xCC, 0x15, 80)
        g.stroke = BasicStroke(6f)
        g.draw(clip)
    }
    
    // --- Shift whole content up slightly to make room for text ---
    val offsetY = -35
    
    // Draw connection lines/paths (network threads)
    g.stroke = BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    
    // Draw golden lines first
    val pathColor = Color(255, 255, 255, 180)
    g.color = pathColor
    g.draw(Line2D.Float(196f, 303f + offsetY, 133f, 370f + offsetY))
    g.draw(Line2D.Float(256f, 322f + offsetY, 256f, 389f + offsetY))
    g.draw(Line2D.Float(316f, 303f + offsetY, 379f, 370f + offsetY))
    
    // Draw Cap Rhombus (Gold)
    val capX = intArrayOf(256, 398, 256, 114)
    val capY = intArrayOf((114 + offsetY).toInt(), (185 + offsetY).toInt(), (256 + offsetY).toInt(), (185 + offsetY).toInt())
    g.color = Color(0xFA, 0xCC, 0x15) // Gold #FACC15
    g.fillPolygon(capX, capY, 4)
    
    // Rhombus thicker contour
    g.color = Color(0xD9, 0x77, 0x06) // Darker gold for depth
    g.stroke = BasicStroke(4f)
    g.drawPolygon(capX, capY, 4)
    
    // Draw Cap Under-Shield Band (Academic ribbon - White)
    val bandPath = GeneralPath()
    bandPath.moveTo(180f, 228f + offsetY)
    bandPath.lineTo(180f, 294f + offsetY)
    bandPath.curveTo(180f, 332f + offsetY, 332f, 332f + offsetY, 332f, 294f + offsetY)
    bandPath.lineTo(332f, 228f + offsetY)
    bandPath.lineTo(256f, 265f + offsetY)
    bandPath.closePath()
    
    g.color = Color.WHITE
    g.fill(bandPath)
    
    // Draw Hanging Tassel
    g.stroke = BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.color = Color.WHITE
    g.draw(Line2D.Float(256f, 185f + offsetY, 133f, 213f + offsetY))
    g.draw(Line2D.Float(133f, 213f + offsetY, 133f, 275f + offsetY))
    
    // Tassel knob (Gold)
    g.color = Color(0xFA, 0xCC, 0x15)
    g.fill(Ellipse2D.Float(119f, 275f + offsetY, 28f, 28f))
    
    // Draw Network Nodes (glowing gold points of connection)
    val nodeColor = Color(0xFA, 0xCC, 0x15)
    g.color = nodeColor
    g.fill(Ellipse2D.Float(112f, 349f + offsetY, 42f, 42f)) // Center 133
    g.fill(Ellipse2D.Float(235f, 368f + offsetY, 42f, 42f)) // Center 256
    g.fill(Ellipse2D.Float(358f, 349f + offsetY, 42f, 42f)) // Center 379
    
    // Add white glow center to the nodes
    g.color = Color.WHITE
    g.fill(Ellipse2D.Float(123f, 360f + offsetY, 20f, 20f))
    g.fill(Ellipse2D.Float(246f, 379f + offsetY, 20f, 20f))
    g.fill(Ellipse2D.Float(369f, 360f + offsetY, 20f, 20f))
    
    // Draw text: "Power Connection" at the bottom
    g.color = Color.WHITE
    g.font = Font("SansSerif", Font.PLAIN, 32)
    val fm1 = g.fontMetrics
    val text1 = "POWER OF"
    val text1X = (baseSize - fm1.stringWidth(text1)) / 2
    g.drawString(text1, text1X, 420)
    
    g.color = Color(0xFA, 0xCC, 0x15) // Golden text for "CONNECTION"
    g.font = Font("SansSerif", Font.BOLD, 46)
    val fm2 = g.fontMetrics
    val text2 = "CONNECTION"
    val text2X = (baseSize - fm2.stringWidth(text2)) / 2
    g.drawString(text2, text2X, 475)
    
    g.dispose()
    
    // Scale down to destination size
    val scaledImg = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gScaled = scaledImg.createGraphics()
    gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    gScaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    gScaled.drawImage(img, 0, 0, size, size, null)
    gScaled.dispose()
    
    return scaledImg
  }
}

