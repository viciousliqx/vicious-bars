package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Step one of resurrecting the bars: just the stone frame + vial look from
 * the old (deleted, recoverable via git history) VitalBarOverlay, landed on
 * screen with real HP data, visible only while Vicious Bars mode is
 * actually on. No icon/counter - that content now lives on the real orb,
 * which will eventually dock onto this bar instead of the bar drawing its
 * own label. No docking yet either - this is a fixed-position proof of
 * life, not the final placement.
 */
@Singleton
class HpVialOverlay extends Overlay
{
	private static final int WIDTH = 20;
	private static final int HEIGHT = 60;
	private static final int BORDER_WIDTH = 2;
	private static final int FRAME_ARC = 6;
	private static final float INNER_ARC_FRACTION = 0.75f;
	private static final int BACKGROUND_ALPHA = 150;

	private static final Color FRAME_FILL_COLOR = new Color(96, 82, 64);
	private static final Color FRAME_OUTLINE_COLOR = new Color(20, 17, 14);
	private static final Color FILL_COLOR = new Color(220, 40, 40);

	private final Client client;
	private final CompactOrbsManager manager;

	@Inject
	private HpVialOverlay(Client client, CompactOrbsManager manager)
	{
		this.client = client;
		this.manager = manager;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!manager.isCompactLayout())
		{
			return null;
		}

		int max = Math.max(client.getRealSkillLevel(Skill.HITPOINTS), 1);
		int current = Math.max(0, Math.min(client.getBoostedSkillLevel(Skill.HITPOINTS), max));

		int innerWidth = WIDTH - 2 * BORDER_WIDTH;
		int innerHeight = HEIGHT - 2 * BORDER_WIDTH;
		int innerArc = Math.round(innerWidth * INNER_ARC_FRACTION);

		drawFrame(graphics, innerWidth, innerHeight, innerArc);
		drawVial(graphics, innerWidth, innerHeight, innerArc, current, max);

		return new Dimension(WIDTH, HEIGHT);
	}

	/** The stone-toned ring between the bar's outer footprint and its inner vial - never filled under the vial itself. */
	private void drawFrame(Graphics2D graphics, int innerWidth, int innerHeight, int innerArc)
	{
		RoundRectangle2D.Float outerShape = new RoundRectangle2D.Float(0, 0, WIDTH, HEIGHT, FRAME_ARC, FRAME_ARC);
		RoundRectangle2D.Float innerShapeAtInset = new RoundRectangle2D.Float(
			BORDER_WIDTH, BORDER_WIDTH, innerWidth, innerHeight, innerArc, innerArc);

		Area frameRing = new Area(outerShape);
		frameRing.subtract(new Area(innerShapeAtInset));

		graphics.setColor(FRAME_FILL_COLOR);
		graphics.fill(frameRing);

		graphics.setColor(FRAME_OUTLINE_COLOR);
		graphics.draw(new RoundRectangle2D.Float(0, 0, WIDTH - 1, HEIGHT - 1, FRAME_ARC, FRAME_ARC));
		graphics.draw(new RoundRectangle2D.Float(BORDER_WIDTH, BORDER_WIDTH, innerWidth - 1, innerHeight - 1, innerArc, innerArc));
	}

	/** Background + colored current/max fill, confined to the inner capsule. */
	private void drawVial(Graphics2D graphics, int innerWidth, int innerHeight, int innerArc, int current, int max)
	{
		Graphics2D vialGraphics = (Graphics2D) graphics.create();
		vialGraphics.translate(BORDER_WIDTH, BORDER_WIDTH);
		vialGraphics.setClip(new RoundRectangle2D.Float(0, 0, innerWidth, innerHeight, innerArc, innerArc));

		vialGraphics.setColor(new Color(0, 0, 0, BACKGROUND_ALPHA));
		vialGraphics.fillRect(0, 0, innerWidth, innerHeight);

		double ratio = (double) current / max;
		int fillHeight = (int) Math.round(ratio * innerHeight);
		vialGraphics.setColor(FILL_COLOR);
		vialGraphics.fillRect(0, innerHeight - fillHeight, innerWidth, fillHeight);

		vialGraphics.dispose();
	}
}
