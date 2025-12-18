import java.util.*;
import java.io.*;

class Node {
    Node left;
    Node right;
    int data;
    
    Node(int data) {
        this.data = data;
        left = null;
        right = null;
    }
}

class Solution {

	/* 
    
    class Node 
    	int data;
    	Node left;
    	Node right;
	*/
	public static void topView(Node root) {
    class QueueNode {
        Node node;
        int level;
        QueueNode(Node node, int level) {
            this.node = node;
            this.level = level;
        }
    }

    if (root == null) return;

    Queue<QueueNode> queue = new LinkedList<>();
    TreeMap<Integer, Integer> map = new TreeMap<>();

    queue.add(new QueueNode(root, 0));

    while (!queue.isEmpty()) {
        QueueNode r = queue.poll();

        if (!map.containsKey(r.level)) {
            map.put(r.level, r.node.data);
        }

        if (r.node.left != null) queue.add(new QueueNode(r.node.left, r.level - 1));
        if (r.node.right != null) queue.add(new QueueNode(r.node.right, r.level + 1));
    }

    for (Integer value : map.values()) {
        System.out.print(value + " ");
    }
}


	public static Node insert(Node root, int data) {
        if(root == null) {
            return new Node(data);
        } else {
            Node cur;
            if(data <= root.data) {
                cur = insert(root.left, data);
                root.left = cur;
            } else {
                cur = insert(root.right, data);
                root.right = cur;
            }
            return root;
        }
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int t = scan.nextInt();
        Node root = null;
        while(t-- > 0) {
            int data = scan.nextInt();
            root = insert(root, data);
        }
        scan.close();
        topView(root);
    }	
}
