package com.jeffrwatts.openglexperiments

import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.floor

class PointClusteringHelper (points: FloatBuffer) {
    companion object {
        private const val GRID_CELL_SIZE = 0.02f // Units: meters.
        private const val MIN_CLUSTER_ELEMENTS = 1
    }

    private var occupancyGrid: Array<Array<BooleanArray>>
    private val gridOriginOffset = FloatArray(3)

    init {
        occupancyGrid = allocateGrid(points)
    }

    fun findClusters(): List<AABB> {
        // Clusters are found by iterating over each cell's neighbors.  As cells are found, they are
        // marked false.  This process continues until all cells are false.
        // Because the original grid is modified, this call will only produce results once.
        val clusters: MutableList<AABB> = ArrayList()
        val currentCluster: MutableList<IntArray> = ArrayList()

        // Searches the grid for clusters.
        val index = IntArray(3)
        index[0] = 0
        while (index[0] < occupancyGrid.size) {
            index[1] = 0
            while (index[1] < occupancyGrid[0].size) {
                index[2] = 0
                while (index[2] < occupancyGrid[0][0].size) {

                    // Attempts to find a cluster containing the current index.
                    depthFirstSearch(index, occupancyGrid, currentCluster)
                    if (currentCluster.size >= MIN_CLUSTER_ELEMENTS) {
                        // Stores the cluster.
                        clusters.add(computeAABB(currentCluster))
                        currentCluster.clear()
                    }
                    ++index[2]
                }
                ++index[1]
            }
            ++index[0]
        }
        return clusters
    }

    private fun allocateGrid(points: FloatBuffer): Array<Array<BooleanArray>> {
        // Finds the min/max bounds of the pointcloud.
        val bounds = AABB()
        points.rewind()
        while (points.hasRemaining()) {
            val x = points.get()
            val y = points.get()
            val z = points.get()
            val confidence = points.get()
            if (confidence <= 0) {
                continue
            }
            bounds.update(x, y, z)
        }

        // Each grid cell is a cube of size GRID_CELL_SIZE^3 cubic meters.
        gridOriginOffset[0] = bounds.minX
        gridOriginOffset[1] = bounds.minY
        gridOriginOffset[2] = bounds.minZ
        val numCellsX = 1.coerceAtLeast(ceil(((bounds.maxX - bounds.minX) / GRID_CELL_SIZE).toDouble()).toInt())
        val numCellsY = 1.coerceAtLeast(ceil(((bounds.maxY - bounds.minY) / GRID_CELL_SIZE).toDouble()).toInt())
        val numCellsZ = 1.coerceAtLeast(ceil(((bounds.maxZ - bounds.minZ) / GRID_CELL_SIZE).toDouble()).toInt())
        val occupancyGrid = Array(numCellsX) {
            Array(numCellsY) {
                BooleanArray(
                    numCellsZ
                )
            }
        }

        // Populates the grid with occupancy of points.
        points.rewind()
        while (points.hasRemaining()) {
            val x = points.get()
            val y = points.get()
            val z = points.get()
            val confidence = points.get()
            if (confidence <= 0) {
                continue
            }

            // Finds the voxel that contains this depth point and sets it to true.
            val indexX = floor(((x - gridOriginOffset[0]) / GRID_CELL_SIZE).toDouble()).toInt()
            val indexY = floor(((y - gridOriginOffset[1]) / GRID_CELL_SIZE).toDouble()).toInt()
            val indexZ = floor(((z - gridOriginOffset[2]) / GRID_CELL_SIZE).toDouble()).toInt()
            occupancyGrid[indexX][indexY][indexZ] = true
        }
        return occupancyGrid
    }

    private fun computeAABB(cluster: List<IntArray>): AABB {
        // Computes the bounds in units of "indices".
        val bounds = AABB()
        for (index in cluster) {
            // The minimum and maximum corners of this grid cell, in units of indices.
            bounds.update(index[0].toFloat(), index[1].toFloat(), index[2].toFloat())
            bounds.update(
                (index[0] + 1).toFloat(), (index[1] + 1).toFloat(),
                (index[2] + 1).toFloat()
            )
        }

        // Rescales units from "indices" to "meters".
        bounds.minX = GRID_CELL_SIZE * bounds.minX + gridOriginOffset[0]
        bounds.minY = GRID_CELL_SIZE * bounds.minY + gridOriginOffset[1]
        bounds.minZ = GRID_CELL_SIZE * bounds.minZ + gridOriginOffset[2]
        bounds.maxX = GRID_CELL_SIZE * bounds.maxX + gridOriginOffset[0]
        bounds.maxY = GRID_CELL_SIZE * bounds.maxY + gridOriginOffset[1]
        bounds.maxZ = GRID_CELL_SIZE * bounds.maxZ + gridOriginOffset[2]
        return bounds
    }

    private fun depthFirstSearch(index: IntArray, grid: Array<Array<BooleanArray>>, cluster: MutableList<IntArray>) {
        if (!inBounds(index, grid) || !grid[index[0]][index[1]][index[2]]) {
            return  // Not occupied, stop searching in this area.
        }

        // Since the current index is occupied, it can be added to the local cluster. Once added,
        // we reset the grid to avoid cyclic behavior.
        grid[index[0]][index[1]][index[2]] = false
        cluster.add(index.clone())

        // Search the neighbors.
        depthFirstSearch(intArrayOf(index[0] - 1, index[1], index[2]), grid, cluster)
        depthFirstSearch(intArrayOf(index[0] + 1, index[1], index[2]), grid, cluster)
        depthFirstSearch(intArrayOf(index[0], index[1] - 1, index[2]), grid, cluster)
        depthFirstSearch(intArrayOf(index[0], index[1] + 1, index[2]), grid, cluster)
        depthFirstSearch(intArrayOf(index[0], index[1], index[2] - 1), grid, cluster)
        depthFirstSearch(intArrayOf(index[0], index[1], index[2] + 1), grid, cluster)
    }

    private fun inBounds(index: IntArray, grid: Array<Array<BooleanArray>>): Boolean {
        return index[0] >= 0 && index[0] < grid.size && index[1] >= 0 && index[1] < grid[0].size && index[2] >= 0 && index[2] < grid[0][0].size
    }
}