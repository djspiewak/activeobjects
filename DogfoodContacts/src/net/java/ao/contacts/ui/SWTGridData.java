/*
 * Created on Jul 29, 2006
 */
package net.java.ao.contacts.ui;

import java.awt.Component;
import java.awt.Dimension;

/**
 * @author Daniel Spiewak
 */
public final class SWTGridData {
	public int verticalAlignment = CENTER;
	public int horizontalAlignment = BEGINNING;
	public int widthHint = DEFAULT;
	public int heightHint = DEFAULT;
	public int horizontalIndent = 0;
	public int verticalIndent = 0;
	public int horizontalSpan = 1;
	public int verticalSpan = 1;
	public boolean grabExcessHorizontalSpace = false;
	public boolean grabExcessVerticalSpace = false;
	public int minimumWidth = 0;
	public int minimumHeight = 0;
	public boolean exclude = false;
	
	public static final int DEFAULT = -1;
	public static final int BEGINNING = 1;
	public static final int CENTER = 2;
	public static final int END = 3;
	public static final int FILL = 4;
	public static final int RIGHT = 5;
	public static final int VERTICAL_ALIGN_BEGINNING =  1 << 1;
	public static final int VERTICAL_ALIGN_CENTER = 1 << 2;
	public static final int VERTICAL_ALIGN_END = 1 << 3;
	public static final int VERTICAL_ALIGN_FILL = 1 << 4;
	public static final int HORIZONTAL_ALIGN_BEGINNING =  1 << 5;
	public static final int HORIZONTAL_ALIGN_CENTER = 1 << 6;
	public static final int HORIZONTAL_ALIGN_END = 1 << 7;
	public static final int HORIZONTAL_ALIGN_FILL = 1 << 8;
	public static final int GRAB_HORIZONTAL = 1 << 9;
	public static final int GRAB_VERTICAL = 1 << 10;
	public static final int FILL_VERTICAL = VERTICAL_ALIGN_FILL | GRAB_VERTICAL;
	public static final int FILL_HORIZONTAL = HORIZONTAL_ALIGN_FILL | GRAB_HORIZONTAL;
	public static final int FILL_BOTH = FILL_VERTICAL | FILL_HORIZONTAL;

	int cacheWidth = -1, cacheHeight = -1;
	int defaultWhint, defaultHhint, defaultWidth = -1, defaultHeight = -1;
	int currentWhint, currentHhint, currentWidth = -1, currentHeight = -1;
	
	public SWTGridData() {
	}
	
	public SWTGridData(int style) {
		super();
		if ((style & VERTICAL_ALIGN_BEGINNING) != 0) {
			verticalAlignment = BEGINNING;
		}
		if ((style & VERTICAL_ALIGN_CENTER) != 0) {
			verticalAlignment = CENTER;
		}
		if ((style & VERTICAL_ALIGN_FILL) != 0) {
			verticalAlignment = FILL;
		}
		if ((style & VERTICAL_ALIGN_END) != 0) {
			verticalAlignment = END;
		}
		if ((style & HORIZONTAL_ALIGN_BEGINNING) != 0) {
			horizontalAlignment = BEGINNING;
		}
		if ((style & HORIZONTAL_ALIGN_CENTER) != 0) {
			horizontalAlignment = CENTER;
		}
		if ((style & HORIZONTAL_ALIGN_FILL) != 0) {
			horizontalAlignment = FILL;
		}
		if ((style & HORIZONTAL_ALIGN_END) != 0) {
			horizontalAlignment = END;
		}
		
		grabExcessHorizontalSpace = (style & GRAB_HORIZONTAL) != 0;
		grabExcessVerticalSpace = (style & GRAB_VERTICAL) != 0;
	}
	
	public SWTGridData(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace) {
		this(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, 1, 1);
	}
	
	public SWTGridData(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int horizontalSpan, int verticalSpan) {
		this.horizontalAlignment = horizontalAlignment;
		this.verticalAlignment = verticalAlignment;
		this.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		this.grabExcessVerticalSpace = grabExcessVerticalSpace;
		this.horizontalSpan = horizontalSpan;
		this.verticalSpan = verticalSpan;
	}
	
	public SWTGridData(int width, int height) {
		super();
		this.widthHint = width;
		this.heightHint = height;
	}

	void computeSize(Component control, int wHint, int hHint) {
		if (cacheWidth != -1 && cacheHeight != -1) {
			return;
		}
		
		if (wHint == this.widthHint && hHint == this.heightHint) {
			if (defaultWidth == -1 || defaultHeight == -1 || wHint != defaultWhint || hHint != defaultHhint) {
				Dimension size = control.getPreferredSize();
				
				if (wHint != DEFAULT) {
					size.width = wHint;
				}
				if (hHint != DEFAULT) {
					size.height = hHint;
				}
				
				defaultWhint = wHint;
				defaultHhint = hHint;
				defaultWidth = size.width;
				defaultHeight = size.height;
			}
			cacheWidth = defaultWidth;
			cacheHeight = defaultHeight;
			return;
		}
		
		if (currentWidth == -1 || currentHeight == -1 || wHint != currentWhint || hHint != currentHhint) {
			Dimension size = control.getPreferredSize();
			
			if (wHint != DEFAULT) {
				size.width = wHint;
			}
			if (hHint != DEFAULT) {
				size.height = hHint;
			}
			
			currentWhint = wHint;
			currentHhint = hHint;
			currentWidth = size.width;
			currentHeight = size.height;
		}
		cacheWidth = currentWidth;
		cacheHeight = currentHeight;
	}

	void flushCache() {
		cacheWidth = cacheHeight = -1;
		defaultWidth = defaultHeight = -1;
		currentWidth = currentHeight = -1;
	}

	String getName() {
		String string = getClass().getName();
		int index = string.lastIndexOf('.');
		if (index == -1)
			return string;
		return string.substring(index + 1, string.length());
	}
}

/*
 ************************************
 * Copyright 2006 Completely Random Solutions 	*
 *                                										*
 * DISCLAIMER:                                					*
 * We are not responsible for any damage      		*
 * directly or indirectly caused by the usage 			*
 * of this or any other class in association  			*
 * with this class.  Use at your own risk.   			*
 * This or any other class by CRS is not				*
 * certified for use in life support systems, by		*
 * Lockheed Martin engineers, in development		*
 * or use of nuclear reactors, weapons of mass    *
 * destruction, or in inter-planetary conflict.			*
 * (Unless otherwise specified)               				*
 ************************************
 */