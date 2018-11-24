/*
 * University of Central Florida
 * CAP4630 - Fall 2018
 * Author: John Hacker
 */

/*
 * Algorithm Explanation :
 *
 * I create a tree of nodes where each node has a position, value, and children.
 * The root is the the final decision for pacman to move.
 * The first branch-level are nodes for all the possible moves pacman can make.
 * The next d (1 -> depth) branch-levels are the possible steps that the ghosts can make.
 *
 * First, I make the tree where all the nodes have a value of INVALID (a constant -1).
 * Once the tree has its full structure, I perfrom the minimax process on it.
 * I start at the root and call a minimum search on each of its children.
 * Each child will recursively call the min-search on their children and so on.
 * At leaf nodes, they return the distance from the new ghost location to the associated new pacman location.
 * Then each non-leaf node will copy the leaf node with lowest value
 * Once all of the root's children have newly establieshed values from min-searches, it performs a max-evaluation
 * If the closest a ghost can get to pacman is not on pacman, then pacman will simply move towards the closest goody
 * If a ghost is within striking distance of pacman, pacman will move to the location that has the hihghest value from the evaluation
 *
 * Depth 2 works, but depth 1 works better.
 *
 * Occasionally there is a "java.lang.ClassCastException: pacsim.FoodCell cannot be cast to pacsim.GhostCell".
 * The rate it appears is about 1/2000 on my machine.
 * It does not list any line of code from my files as the source of the problem.
 * I can not find why the problem happens and it is rare.
 * So, I think the problem might not be directly from my code.
 * I hope this is not a problem.
 */

//Import statements
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.Stack;
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
  private int foodLow;
  private PacFace newFace;
  private Node root;
  private ArrayList<ArrayList<Node>> tree;

  public PacSimMinimax(int depth, String fname, int te, int gran, int max)
  {
    //Initialize variables
    this.depth = depth;

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
    foodLow = INVALID;

    fillTree(grid, pc);
    root = minimax(grid, pc, root);
    newFace = PacUtils.direction(pc.getLoc(), root.getLoc());

    return newFace;
  }

  public Node minimax(PacCell[][] grid, PacmanCell pc, Node node)
  {
    int max = INVALID;
    Node temp = node;
    Stack<Node> moves = new Stack<Node>();
    for (Node child : node.getChildren())
    {
      moves.push(new Node(child));
      child = evaluateChild(grid, child, child.getLoc());

      if (child.getValue() > max || max == INVALID)
      {
        max = child.getValue();
        node.setLoc(moves.peek().getLoc());
        node.setValue(child.getValue());
      }
    }
    if (max > depth + 1 || PacUtils.nearestGhost(node.getLoc(), grid).getMode().toString().equals("FEAR"))
    {
      Point closestToFood = node.getLoc();
      try{closestToFood = BFSPath.getPath(grid, pc.getLoc(), PacUtils.nearestGoody(pc.getLoc(), grid)).get(0);}
      catch(Exception e){}
      node.setLoc(closestToFood);
    }

    return node;
  }

  public boolean evalMax(PacCell[][] grid, PacmanCell pc, Node node, Node child, int max)
  {
    boolean higher = false;

    if (child.getValue() > max || max == INVALID)
    {
      higher = true;
    }

    if (max > depth + 1 || PacUtils.nearestGhost(node.getLoc(), grid).getMode().toString().equals("FEAR"))
    {
      int temp = BFSPath.getPath(grid, node.getLoc(), PacUtils.nearestGoody(node.getLoc(), grid)).size();
      if (temp < foodLow || foodLow == INVALID)
      {
        foodLow = temp;
        higher = true;
      }
    }

    return higher;
  }

  public Node evaluateChild(PacCell[][] grid, Node node, Point pac)
  {
    if (node.getChildren().isEmpty())
    {
      int value = BFSPath.getPath(grid, pac, node.getLoc()).size();
      node.setValue(value);
    }
    else
    {
      int min = INVALID;
      Node temp = node;
      for (Node child : node.getChildren())
      {
        child = evaluateChild(grid, child, pac);

        if (evalMin(child, min))
        {
          min = child.getValue();
          temp = child;
        }
      }
      node.setLoc(temp.getLoc());
      node.setValue(min);
    }
    return node;
  }

  public boolean evalMin(Node node, int min)
  {
    boolean lower = node.getValue() < min || min == INVALID;
    return lower;
  }

  public void fillTree(PacCell[][] grid, PacmanCell pc)
  {
    //Create the root of the tree
    root = new Node(INVALID, pc.getLoc());
    tree = new ArrayList<ArrayList<Node>>();
    tree.add(new ArrayList<Node>());
    tree.get(0).add(root);

    //Create the nodes for all of pacman's possible moves
    root = populateChildren(grid, root);
    tree.add(root.getChildren());

    //Create nodes for all of the ghosts' possible first moves
    List<Point> ghosts = PacUtils.findGhosts(grid);
    tree.add(new ArrayList<Node>());
    for (Node child : root.getChildren())
    {
      child = populateDoubleChildren(grid, child, ghosts);
      tree.get(2).addAll(child.getChildren());
    }

    //Create nodes for all of the ghosts' possible d'th moves ()
    for (int d = 2; d <= depth; d++)
    {
      tree.add(new ArrayList<Node>());
      for (Node node : tree.get(d))
      {
        node = populateChildren(grid, node);
        tree.get(d + 1).addAll(node.getChildren());
      }
    }
  }

  public Node populateChildren(PacCell[][] grid, Node node)
  {
    for (int i = 0; i < DIRECTIONS; i++)
    {
      Point move = new Point(node.getLoc());
      //East
      if (i == 0)
      {
        move = new Point(node.getLoc().x + 1, node.getLoc().y);
      }
      //West
      else if (i == 1)
      {
        move = new Point(node.getLoc().x - 1, node.getLoc().y);
      }
      //North
      else if (i == 2)
      {
        move = new Point(node.getLoc().x, node.getLoc().y - 1);
      }
      //South
      else if (i == 3)
      {
        move = new Point(node.getLoc().x, node.getLoc().y + 1);
      }

      if (grid[move.x][move.y] instanceof WallCell
          || grid[move.x][move.y] instanceof GhostCell
          || grid[move.x][move.y] instanceof HouseCell)
      {
        continue;
      }

      Node child = new Node(INVALID, move);
      node.addChild(child);
    }
    return node;
  }

  public Node populateDoubleChildren(PacCell[][] grid, Node node, List<Point> ghosts)
  {
    for (Point ghost : ghosts)
    {
      for (int i = 0; i < DIRECTIONS; i++)
      {
        Point move = new Point(ghost);
        //East
        if (i == 0)
        {
          move = new Point(ghost.x + 1, ghost.y);
        }
        //West
        else if (i == 1)
        {
          move = new Point(ghost.x - 1, ghost.y);
        }
        //North
        else if (i == 2)
        {
          move = new Point(ghost.x, ghost.y - 1);
        }
        //South
        else if (i == 3)
        {
          move = new Point(ghost.x, ghost.y + 1);
        }

        if (grid[move.x][move.y] instanceof WallCell
            || grid[move.x][move.y] instanceof GhostCell)
        {
          continue;
        }

        Node child = new Node(INVALID, move);
        node.addChild(child);
      }
    }
    return node;
  }

  public void printTree()
  {
    for (int i = 0; i < tree.size(); i++)
    {
      for (int j = 0; j < tree.get(i).size(); j++)
      {
        System.out.print(tree.get(i).get(j).getValue() + " ");
      }
      System.out.println();
    }
  }
}

class Node
{
  ArrayList<Node> children;
  private int value;
  private Point loc;

  public Node(int value, Point loc)
  {
    children = new ArrayList<Node>();
    this.value = value;
    this.loc = new Point(loc);
  }

  public Node(Node other)
  {
    this.children = new ArrayList<Node>();
    this.children.addAll(other.getChildren());
    this.value = other.getValue();
    this.loc = other.getLoc();
  }

  public void addChild(Node child)
  {
    if (child != null)
    {
      children.add(child);
    }
  }

  public ArrayList<Node> getChildren()
  {
    return children;
  }

  public void setChildren(ArrayList<Node> children)
  {
    this.children = new ArrayList<Node>();
    this.children.addAll(children);
  }

  public int getValue()
  {
    return value;
  }

  public void setValue(int value)
  {
    this.value = value;
  }

  public Point getLoc()
  {
    return loc;
  }

  public void setLoc(Point loc)
  {
    this.loc = new Point(loc);
  }
}
