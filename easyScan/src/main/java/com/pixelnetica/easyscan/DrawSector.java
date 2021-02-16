package com.pixelnetica.easyscan;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Data set for Canvas.drawArc
 * @author Denis
 *
 */
public class DrawSector {
	private PointF center;
	private float radius;
	private float startAngle;
	private float finishAngle;
	private Paint paint;

	public DrawSector(PointF center, float radius, float startAngle, float finishAngle, Paint paint)
	{
		this.center = new PointF();
		this.center.set(center);
		this.radius = radius;
		this.startAngle = startAngle;
		this.finishAngle = finishAngle;
		this.paint = paint;
	}

	public void setPainter(Paint paint)
	{
		this.paint = paint;
	}

	public void draw(Canvas canvas)
	{
		float sweepAngle = finishAngle - startAngle;
		if (sweepAngle < 0)
		{
			sweepAngle += 360f;
		}
		canvas.drawArc(describedRect(), startAngle, sweepAngle, true, paint);
	}

	private RectF describedRect()
	{
		return new RectF(
				center.x - radius,
				center.y - radius,
				center.x + radius,
				center.y + radius);
	}

}
