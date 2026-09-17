/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.backpacksplus.domain;

/** AF: measured item vertices in model space. RI: finite, ordered, nondegenerate extents. */
public record ModelBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public enum Plane { XY, YZ, XZ }
    public ModelBounds {
        if (!Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ
                || Math.max(maxX-minX, Math.max(maxY-minY, maxZ-minZ)) < 0.00001)
            throw new IllegalArgumentException("Empty or invalid rendered bounds");
    }
    public double width() { return maxX-minX; }
    public double height() { return maxY-minY; }
    public double depth() { return maxZ-minZ; }
    public double centerX() { return (minX+maxX)/2; }
    public double centerY() { return (minY+maxY)/2; }
    public double centerZ() { return (minZ+maxZ)/2; }
    /** effects: returns the broad face of a predominantly flat model. */
    public Plane plane() {
        if (width()<depth()*0.6 && width()<height()*0.6) return Plane.YZ;
        if (height()<depth()*0.6 && height()<width()*0.6) return Plane.XZ;
        return Plane.XY;
    }
    /** requires: positive finite length; effects: fits the entire model diagonal within the limit. */
    public double fit(double length) {
        if (!Double.isFinite(length) || length<=0) throw new IllegalArgumentException("Invalid length");
        return length/Math.sqrt(width()*width()+height()*height()+depth()*depth());
    }
    public boolean squareSprite() { return depth()<Math.max(width(),height())*0.15 && width()>height()*0.75; }
}
