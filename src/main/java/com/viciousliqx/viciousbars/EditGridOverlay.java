package com.viciousliqx.viciousbars;

import com.compactorbs.CompactOrbsConfig;
import com.compactorbs.CompactOrbsManager;
import com.compactorbs.widget.layout.edit.EditManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws reference grid lines over the red edit-mode drag zone (the native
 * RECTANGLE widget EditManager creates - see its own updateBackground()) -
 * purely visual, no snapping, doesn't touch drag positions at all. The red
 * zone is a real widget, so this needs ABOVE_WIDGETS specifically (our
 * bars default to UNDER_WIDGETS, which would draw this grid behind it and
 * never show).
 */
@Singleton
class EditGridOverlay extends Overlay
{
	private static final int GRID_SPACING = 16;
	private static final Color GRID_COLOR = new Color(255, 255, 255, 60);

	private final CompactOrbsManager manager;
	private final CompactOrbsConfig config;
	private final EditManager editManager;

	@Inject
	private EditGridOverlay(CompactOrbsManager manager, CompactOrbsConfig config, EditManager editManager)
	{
		this.manager = manager;
		this.config = config;
		this.editManager = editManager;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
		setMovable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!manager.isEditingLayout || !config.showEditGrid())
		{
			return null;
		}

		Widget zone = editManager.getEditBackground();
		if (zone == null || zone.isHidden())
		{
			return null;
		}

		Point canvas = zone.getCanvasLocation();
		if (canvas == null)
		{
			return null;
		}

		int width = zone.getWidth();
		int height = zone.getHeight();
		int left = canvas.getX();
		int top = canvas.getY();

		graphics.setColor(GRID_COLOR);
		for (int x = 0; x <= width; x += GRID_SPACING)
		{
			graphics.drawLine(left + x, top, left + x, top + height);
		}
		for (int y = 0; y <= height; y += GRID_SPACING)
		{
			graphics.drawLine(left, top + y, left + width, top + y);
		}

		return null;
	}
}
