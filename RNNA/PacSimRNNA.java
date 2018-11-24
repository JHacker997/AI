/*
 *  University of Central Florida
 *  CAP4630 - Fall 2018
 *  Author: John Hacker
 */

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import pacsim.BFSPath;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;

/**
 * Repetitive Nearest Neighbor Algorithm
 * @author John Hacker
 */
public class PacSimRNNA implements PacAction
{
   private List<Point> path;
   private int simTime;
   private boolean started;
   private long startTime;
   private List<Point> foods;
   private int[][] costTable;

   public PacSimRNNA( String fname )
   {
      PacSim sim = new PacSim( fname );
      sim.init(this);
   }

   public static void main( String[] args )
   {
      System.out.println("\nTSP using Repetitive Nearest Neighbor Algorithm by John Hacker:");
      System.out.println("\nMaze : " + args[ 0 ] + "\n" );
      new PacSimRNNA( args[ 0 ] );
   }

   @Override
   public void init()
   {
      simTime = 0;
      path = new ArrayList();
      started = false;
   }

   @Override
   public PacFace action( Object state )
   {
      PacCell[][] grid = (PacCell[][]) state;
      PacmanCell pc = PacUtils.findPacman( grid );

      //Make sure Pac-Man is in this game
      if( pc == null ) return null;

      if (!started)
      {
         started = true;
         createCostTable(grid);
         printFoods();
         findOptimalPath();
         System.out.println("Solution moves:\n");
      }

      if( path.isEmpty() )
      {
         Point tgt = foods.remove(0);
         path = BFSPath.getPath(grid, pc.getLoc(), tgt);
      }

      //Take the next step on the current path
      Point next = path.remove( 0 );
      PacFace face = PacUtils.direction( pc.getLoc(), next );
      System.out.printf( "%5d : From [ %2d, %2d ] go %s%n",
                        ++simTime, pc.getLoc().x, pc.getLoc().y, face );

      return face;
   }


   public void createCostTable(PacCell[][] grid)
   {
      costTable = new int[PacUtils.numFood(grid) + 1][PacUtils.numFood(grid) + 1];
      foods = PacUtils.findFood(grid);
      foods.add(0, PacUtils.findPacman(grid).getLoc());

      //Cost Table
      System.out.println("\nCost table:\n");
      for (int r = 0; r < foods.size(); r++)
      {
          for (int c = 0; c < foods.size(); c++)
          {
              costTable[r][c] = BFSPath.getPath(grid, foods.get(r), foods.get(c)).size();
          }
      }
      for (int r = 0; r < costTable.length; r++)
      {
          for (int c = 0; c < costTable[0].length; c++)
          {
              System.out.printf("%4d", costTable[r][c]);
          }
          System.out.println();
      }
   }


   public void printFoods()
   {
      //Food Array
      System.out.println("\n\nFood Array:\n");
      for (int i = 0; i < foods.size() - 1; i++)
      {
          System.out.println(i + " : (" + foods.get(i + 1).x + "," + foods.get(i + 1).y + ")");
      }
      System.out.println();
   }


   public void findOptimalPath()
   {
      //Population Entries
      final long startTime = System.currentTimeMillis();
      List<Path> paths = new ArrayList();
      int temp = 0;
      for (int p = 1; p < foods.size(); p++)
      {
          paths.add(new Path(costTable, p, foods));
      }
      System.out.println("\nPopulation at step 1 :");
      Collections.sort(paths);
      for (int p = 0; p < paths.size(); p++)
      {
          paths.get(p).printString(p);
      }
      for (int s = 2; s < foods.size(); s++)
      {
          System.out.println("\nPopulation at step " + s + " :");
          temp = paths.size();
          for (int p = 0; p < temp; p++)
          {
             paths.addAll(paths.get(p).nearestNeighbor());
          }
          Collections.sort(paths);
          for (int p = 0; p < paths.size(); p++)
          {
             paths.get(p).printString(p);
          }
      }
      final long endTime = System.currentTimeMillis();
      System.out.println("\nTime to generate plan: " + (endTime - startTime) + " msec\n");

      foods = paths.get(0).reorderFoods();
   }
}


class Path implements Comparable<Path>
{
    private int[][] costTable;
    private int cost;
    private int row;
    private int col;
    private List<Integer> costs;
    private List<Point> foods;
    private List<Integer> found;
    private boolean rowOverCol;

    public Path(int[][] table, int first, List<Point> allFoods)
    {
        costTable = cloneTable(table);
        cost = costTable[0][first];
        for (int f = 0; f < costTable[0].length; f++)
        {
            costTable[0][f] = 0;
            costTable[f][0] = 0;
        }
        row = 0;
        col = first;
        costs = new ArrayList();
        costs.add(cost);
        foods = new ArrayList();
        foods.addAll(allFoods);
        found = new ArrayList();
        found.add(first);
        rowOverCol = false;
    }


    public Path(Path other, int index)
    {
        this.costTable = cloneTable(other.costTable);

        this.cost = other.cost;

        this.row = other.row;
        this.col = other.col;
        this.rowOverCol = other.rowOverCol;
        this.clearUsed(index);

        this.costs = new ArrayList();
        this.costs.addAll(other.costs);

        this.foods = new ArrayList();
        this.foods.addAll(other.foods);

        this.found = new ArrayList();
        this.found.addAll(other.found);
        found.add(index);

        this.rowOverCol = !other.rowOverCol;
    }


    public List<Path> nearestNeighbor()
    {
        int low = 0;
        List<Integer> index = new ArrayList();

        //Find the lowest cost
        for (int to = 0; to < costTable.length; to++)
        {
            if (rowOverCol)
            {
                if (low == 0 && costTable[row][to] > 0)
                {
                    low = costTable[row][to];
                    index.add(to);
                }
                else if (costTable[row][to] != 0 && costTable[row][to] < low)
                {
                    low = costTable[row][to];
                    index.clear();
                    index.add(to);
                }
                else if (costTable[row][to] == low && low != 0)
                {
                    index.add(to);
                }
            }
            else
            {
                if (low == 0 && costTable[to][col] > 0)
                {
                    low = costTable[to][col];
                    index.add(to);
                }
                else if (costTable[to][col] != 0 && costTable[to][col] < low)
                {
                    low = costTable[to][col];
                    index.clear();
                    index.add(to);
                }
                else if (costTable[to][col] == low && low != 0)
                {
                    index.add(to);
                }
            }
        }

        //Erase the lowest cost from future possibilities
        List<Path> branchPaths = new ArrayList();
        Path branch;
        for (int i = 0; i < index.size(); i++)
        {
            if (i == 0)
            {
                costs.add(low);
                cost += low;
            }
            else
            {
                branch = new Path(this, index.get(i));
                branchPaths.add(branch);
            }
        }
        if (index.size() > 0)
        {
            found.add(index.get(0));
            clearUsed(index.get(0));
        }
        rowOverCol = !rowOverCol;

        return branchPaths;
    }


    public void clearUsed(int index)
    {
        for (int f = 0; f < costTable.length; f++)
        {
            if (rowOverCol)
            {
                col = index;
                costTable[row][f] = 0;
                costTable[f][row] = 0;
            }
            else
            {
                row = index;
                costTable[f][col] = 0;
                costTable[col][f] = 0;
            }
        }
        if (rowOverCol)
        {
            col = index;
        }
        else
        {
            row = index;
        }
    }


    public List<Point> reorderFoods()
    {
        //List the foods in optimal order
        List<Point> temp = new ArrayList();
        temp.addAll(foods);
        foods.clear();
        for (int f = 0; f < temp.size() - 1; f++)
        {
            foods.add(temp.get(found.get(f)));
        }

        return foods;
    }


    public int[][] cloneTable(int[][] table)
    {
        int[][] costTable = new int[table.length][table.length];
        for (int r = 0; r < costTable.length; r++)
        {
            for (int c = 0; c < costTable.length; c++)
            {
                costTable[r][c] = table[r][c];
            }
        }
        return costTable;
    }


    public void printString(int n)
    {
        System.out.printf("   %d : cost=%d : ", n, cost);
        for (int c = 0; c < costs.size(); c++)
        {
            System.out.printf("[(%d,%d),%d]", foods.get(found.get(c)).x, foods.get(found.get(c)).y, costs.get(c));
        }
        System.out.println();
    }


    public void printTable()
    {
        for (int r = 0; r < costTable.length; r++)
        {
            for (int c = 0; c < costTable[0].length; c++)
            {
                System.out.printf("%4d", costTable[r][c]);
            }
            System.out.println();
        }
    }


    @Override
    public int compareTo(Path other)
    {
        return this.cost - other.cost;
    }
}
