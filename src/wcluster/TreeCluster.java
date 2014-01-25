package wcluster;

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   TreeCluster.java

import java.io.PrintStream;

public class TreeCluster
{
    public class TreeNode
    {

        public void printNode()
        {
            System.out.print(value);
            System.out.print(":");
            if(left != null)
            {
                left.printNode();
                System.out.print(",");
            }
            if(right != null)
                right.printNode();
            System.out.print(".");
        }

        double distance;
        int value;
        TreeNode left;
        TreeNode right;
        TreeNode parent;

        public TreeNode()
        {
            distance = 0.0D;
            value = 0;
            left = null;
            right = null;
            parent = null;
        }

        public TreeNode(int v, TreeNode parent, int tree[][], double dist[])
        {
            distance = 0.0D;
            value = v;
            left = null;
            right = null;
            if(v < 0)
            {
                distance = dist[-(v + 1)];
                left = new TreeNode(tree[-(v + 1)][0], this, tree, dist);
                right = new TreeNode(tree[-(v + 1)][1], this, tree, dist);
            }
        }
    }


    public TreeCluster()
    {
        root = null;
        orderedIdxs = null;
    }

    public TreeCluster(int tree[][], double dist[])
    {
        if(tree != null)
        {
            int n = tree.length;
            root = new TreeNode(-n, null, tree, dist);
            orderedIdxs = new int[n + 1];
            traverseTree(0, root);
        } else
        {
            root = new TreeNode();
        }
    }

    public int traverseTree(int idx, TreeNode node)
    {
        if(node.left != null && node.right != null)
        {
            idx = traverseTree(idx, node.left);
            idx = traverseTree(idx, node.right);
            return idx;
        } else
        {
            orderedIdxs[idx] = node.value;
            return ++idx;
        }
    }

    public void printTree()
    {
        root.printNode();
    }

    TreeNode root;
    int orderedIdxs[];
}
