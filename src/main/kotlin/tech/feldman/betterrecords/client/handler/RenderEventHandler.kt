/**
 * The MIT License
 *
 * Copyright (c) 2019 Nicholas Feldman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package tech.feldman.betterrecords.client.handler

import tech.feldman.betterrecords.ID
import tech.feldman.betterrecords.ModConfig
import tech.feldman.betterrecords.api.wire.IRecordWireHome
import tech.feldman.betterrecords.client.sound.SoundPlayer
import tech.feldman.betterrecords.extensions.glMatrix
import tech.feldman.betterrecords.item.ItemWire
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import org.lwjgl.opengl.GL11

@Mod.EventBusSubscriber(modid = ID, value = [Side.CLIENT])
object RenderEventHandler {

    @SubscribeEvent
    fun onRenderEvent(event: RenderWorldLastEvent) {
        val mc = Minecraft.getMinecraft()
        ItemWire.connection?.let {
            if (mc.player.heldItemMainhand.isEmpty || mc.player.heldItemMainhand.item !is ItemWire) {
                ItemWire.connection = null
            } else {
                glMatrix {
                    GlStateManager.disableTexture2D()

                    val dx = (mc.player.prevPosX + (mc.player.posX - mc.player.prevPosX) * event.partialTicks).toFloat()
                    val dy = (mc.player.prevPosY + (mc.player.posY - mc.player.prevPosY) * event.partialTicks).toFloat()
                    val dz = (mc.player.prevPosZ + (mc.player.posZ - mc.player.prevPosZ) * event.partialTicks).toFloat()
                    val x1 = -(dx - if (ItemWire.connection!!.fromHome) ItemWire.connection!!.x1 else ItemWire.connection!!.x2)
                    val y1 = -(dy - if (ItemWire.connection!!.fromHome) ItemWire.connection!!.y1 else ItemWire.connection!!.y2)
                    val z1 = -(dz - if (ItemWire.connection!!.fromHome) ItemWire.connection!!.z1 else ItemWire.connection!!.z2)

                    GlStateManager.translate(x1 + 0.5F, y1 + 0.5F, z1 + 0.5F)
                    GlStateManager.glLineWidth(2F)
                    GlStateManager.color(0F, 0F, 0F)

                    GlStateManager.glBegin(GL11.GL_LINE_STRIP)
                    GlStateManager.glVertex3f(0F, 0F, 0F)
                    GlStateManager.glVertex3f(0F, 3F, 0F)
                    GlStateManager.glEnd()

                    if (ModConfig.client.devMode && ItemWire.connection!!.fromHome) {
                        // TODO: Clean up this
                        val pos = BlockPos(ItemWire.connection!!.x1, ItemWire.connection!!.y1, ItemWire.connection!!.z1)
                        if (SoundPlayer.isSoundPlayingAt(pos, mc.world.provider.dimension)) {
                            val radius = (mc.world.getTileEntity(pos) as IRecordWireHome).songRadius

                            GlStateManager.disableCull()
                            GlStateManager.enableBlend()
                            GlStateManager.color(0.1F, 0.1F, 0.1F, 0.2F)

                            GlStateManager.glBegin(GL11.GL_LINE_STRIP)
                            GL11.glVertex2f(0F, 0F)
                            GL11.glVertex2f(0F, radius + 10F)
                            GlStateManager.glEnd()

                            val factor = Math.PI * 2 / 45

                            // TODO: Eliminate nested function
                            fun draw(rad: Float) {
                                var phi = 0.0
                                while (phi <= Math.PI / 1.05) {
                                    GL11.glBegin(GL11.GL_QUAD_STRIP)
                                    var theta = 0.0
                                    while (theta <= Math.PI * 2 + factor) {
                                        var x = rad.toDouble() * Math.sin(phi) * Math.cos(theta)
                                        var y = -rad * Math.cos(phi)
                                        var z = rad.toDouble() * Math.sin(phi) * Math.sin(theta)
                                        GL11.glVertex3d(x, y, z)
                                        x = rad.toDouble() * Math.sin(phi + factor) * Math.cos(theta)
                                        y = -rad * Math.cos(phi + factor)
                                        z = rad.toDouble() * Math.sin(phi + factor) * Math.sin(theta)
                                        GL11.glVertex3d(x, y, z)
                                        theta += factor
                                    }
                                    GL11.glEnd()
                                    phi += factor
                                }
                            }

                            draw(radius)
                            val volumeRadius = radius * (Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER) * Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS))
                            GlStateManager.color(1F, 0.1F, 0.1F, 0.2F)
                            draw(volumeRadius)

                            GlStateManager.disableBlend()
                            GlStateManager.enableCull()
                        }
                    }
                    GlStateManager.color(1F, 1F, 1F)
                    GlStateManager.enableTexture2D()
                }
            }
        }
    }
}
