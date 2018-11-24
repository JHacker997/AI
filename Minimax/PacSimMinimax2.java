/*
 * University of Central Florida
 * CAP4630 - Fall 2018
 * Author: John Hacker
 */

//Import statements
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
import pacsim.*;

public class PacSimMinimax implements PacAction
{
  //Member variables
  private static final int INVALID = -1;
  private static final int DIRECTIONS = 4;
  private Point next;
  private int depth;
  private int dCounter;
  private PacFace newFace;
  private Node root;

  public PacSimMinimax(int depth, String fname, int te, int gran, int max)
  {
    //Initialize variables
    this.depth = depth;
    dCounter = depth;

    PacSim sim = new PacSim(fname, te, gran, max);
    sim.init(this);
  }

  public static void main(String[] args)
  {
    String fname = args[0];
    int depth = Integer.parseInt(args[1]);

    int te = 0;
    int gr = 0;
    int ml = 0;

    if (args.length == 5)
    {
      te = Integer.parseInt(args[2]);
      gr = Integer.parseInt(args[3]);
      ml = Integer.parseInt(args[4]);
    }

    new PacSimMinimax(depth, fname, te, gr, ml);

    System.out.println("\nAdversarial Search using Minimax by John Hacker:");
    System.out.println("\n   Game board   : " + fname);
    System.out.println("   Search depth : " + depth + "\n");

    if (te > 0)
    {
      System.out.println("   Preliminary runs : " + te
                         + "\n   Granularity      : " + gr
                         + "\n   Max move limit   : " + ml
                         + "\n\nPreliminary run results :\n");
    }
  }

  @Override
  public void init()
  {

  }

  @Override
  public PacFace action(Object state)
  {
    PacCell[][] grid = (PacCell[][]) state;
    newFace = null;
    PacmanCell pc = PacUtils.findPacman( grid );
    root = null;
    dCounter = depth;

    //Your code goes here
    newFace = nextMove(grid, pc);

    return newFace;
  }

  public PacFace nextMove(PacCell[][] grid, PacmanCell pc)
  {
    PacFace newFace = PacUtils.direction(pc.getLoc(), minimax(grid, pc.getLoc(), INVALID));
    return newFace;
  }

  public Point minimax(PacCell[][] grid, Point loc, int value)
  {
//System.out.println("minimax");
    if (root == null)
    {
//System.out.println("null root");
      root = new Node(INVALID, loc);
      return minimax(grid, root.getLoc(), root.getValue());
    }
    Point move = new Point(loc);
    ArrayList<Point> mins = new ArrayList<Point>();
//System.out.println("depth = " + depth);
    if (dCounter > 0)
    {
//System.out.println("dCounter = " + dCounter);
      Point current = new Point(loc);
      int max = INVALID, min = INVALID, temp = INVALID;
      List<Point> ghosts = PacUtils.findGhosts(grid);

//System.out.println("num of ghosts = " + ghosts.size());
//System.out.println("Pacman: (" + current.x + ", " + current.y + ")");
      //Loop through all of PacMan's moves
      for (int i = 0; i < DIRECTIONS; i++)
      {
        //East
        if (i == 0)
        {
//System.out.print("  pc East");
          current.move(loc.x + 1, loc.y);
        }
        //West
        else if (i == 1)
        {
//System.out.print("  pc West");
          current.move(loc.x - 1, loc.y);
        }
        //North
        else if (i == 2)
        {
//System.out.print("  pc North");
          current.move(loc.x, loc.y - 1);
        }
        //South
        else if (i == 3)
        {
//System.out.print("  pc South");
          current.move(loc.x, loc.y + 1);
        }

        //Check if the move is possible
        if (grid[current.x][current.y] instanceof WallCell
            || grid[current.x][current.y] instanceof GhostCell
            || grid[current.x][current.y] instanceof HouseCell)
        {
//System.out.println(" INVALID");
          continue;
        }
//System.out.println();
        //Loop through all of the ghosts
        for (Point ghost : ghosts)
        {
          Point gMove = new Point(ghost);
//System.out.println("(" + ghost.x + ", " + ghost.y + ")");
          //Loop through all of the ghost's moves
          for (int j = 0; j < DIRECTIONS; j++)
          {
            //East
            if (j == 0)
            {
              gMove.move(ghost.x + 1, ghost.y);
            }
            //West
            else if (j == 1)
            {
              gMove.move(ghost.x - 1, ghost.y);
            }
            //North
            else if (j == 2)
            {
              gMove.move(ghost.x, ghost.y - 1);
            }
            //South
            else if (j == 3)
            {
              gMove.move(ghost.x, ghost.y + 1);
            }

//System.out.print(gMove.toString());
            //Check if the move is possible
            if (grid[gMove.x][gMove.y] instanceof WallCell
                || grid[gMove.x][gMove.y] instanceof GhostCell)
            {
//System.out.println(" Invalid");
              continue;
            }

            //Find the distance between pacman's and the ghost's moves
            temp = BFSPath.getPath(grid, gMove, current).size();
//System.out.println(" -> " + current.toString() + " = " + temp + " moves");

            if (temp < min || min == INVALID)
            {
              min = temp;
            }
//System.out.println("min = " + min);
          }
//System.out.println();
        }
//         if (max == min || min > 2)
//         {
// System.out.print(".");
//           mins.add(new Point(current));
//         }
//         int val = min - BFSPath.getPath(grid, current, PacUtils.nearestFood(current, grid)).size();
        // int val = evaluateMax(grid, current, min);
        if (min > max)
        {
          max = min;
          move = new Point(current);
          mins.clear();
        }
//System.out.println("max = " + max);
        min = INVALID;
      }
      if (max > 2)
      {
        Point closestFood = loc;
        try{closestFood = BFSPath.getPath(grid, loc, PacUtils.nearestGoody(loc, grid)).get(0);}
        catch(Exception e){}
        return closestFood;
      }
    }
    int smallest = INVALID;
    for (Point m : mins)
    {
// System.out.print(",");
      int hold = BFSPath.getPath(grid, m, PacUtils.nearestFood(m, grid)).size();
      if (hold < smallest || smallest == INVALID)
      {
// System.out.print(hold);
        smallest = hold;
        move = m;
      }
    }

//System.out.println();
    return move;
  }

  public int evaluateMax(PacCell[][] grid, Point cur, int min)
  {
    int val = INVALID;
    if (min != INVALID && min < 3)
    {
      val += min * 100;
    }

    val += BFSPath.getPath(grid, cur, PacUtils.nearestFood(cur, grid)).size();

    return val;
  }
}

class Node
{
  private ArrayList<Node> children = null;
  private int value;
  private Point loc;

  public Node(int value, Point loc)
  {
    this.children = new ArrayList<>();
    this.value = value;
    this.loc = loc;
  }

  public void addChild(Node child)
  {
    children.add(child);
  }

  public int getValue()
  {
    return value;
  }

  public Point getLoc()
  {
    return loc;
  }
}
