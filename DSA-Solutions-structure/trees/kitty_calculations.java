import java.io.*;
import java.util.*;

public class Solution {

    static ArrayList<ArrayList<Integer>> edges = new ArrayList<ArrayList<Integer>>();
    static int[] nodeDepth;
    static int[][] nodeLinks;
    static boolean[] nodeOccupied; 
    static long[] nodeSum;
    static long totalResult;
    static final long MODMAX = 1000000007;

    public static void main(String[] args) {

        Reader scan = new Reader();
        
        int N = scan.nextInt(); 
        int Q = scan.nextInt(); 

        for (int i = 0; i <= N; i++) edges.add(new ArrayList<Integer>());
        edges.get(1).add(0);

        for (int i = 0; i < N - 1; i++) {
            int a = scan.nextInt();
            int b = scan.nextInt();
            edges.get(a).add(b);
            edges.get(b).add(a);
        }

        processTree(N);

        StringBuilder sb = new StringBuilder(Q * 10);

        for (int q = 0; q < Q; q++) {
            int K = scan.nextInt();

            if (K <= 1) {
                scan.nextInt();
                sb.append('0').append('\n');
                continue;
            }

            nodeOccupied = new boolean[N + 1];  
            nodeSum = new long[N + 1]; 

            long kSum = 0;
            int maxDepth = 0;
            ArrayList<Integer> fullNodeList = new ArrayList<>(K + 1);

            for (int k = 0; k < K; k++) {
                int node = scan.nextInt();
                int depth = nodeDepth[node];
                if (depth > maxDepth) maxDepth = depth;

                kSum += node;
                nodeOccupied[node] = true;
                nodeSum[node] = node;
                fullNodeList.add(node);
            }

            long totalWithoutLCA = computeFullDepthResult(fullNodeList, K, kSum);

            totalResult = 0;
            computeSubtreeResult(fullNodeList, 0, maxDepth, 1);
            long totalOnlyLCA = totalResult;

            long result = combineResults(totalWithoutLCA, totalOnlyLCA);

            sb.append(result).append('\n');
        }

        System.out.println(sb);
    }


    // ----------------- SUPPORT FUNCTIONS BELOW -----------------

    static final void computeSubtreeResult(ArrayList<Integer> nodesInSubtree, int topDepth, int bottomDepth, 
            int rootNode) {

        int subtreeSize = nodesInSubtree.size();
        if (subtreeSize == 1) {
            nodeOccupied[rootNode] = true;
            nodeSum[rootNode] = nodeSum[nodesInSubtree.get(0)];
            return;
        }

        int height = bottomDepth - topDepth;
        if (height == 1) {
            int onChild = 0;
            if (!nodeOccupied[rootNode]) {
                int childNode = nodesInSubtree.get(0);
                nodeOccupied[rootNode] = true;
                nodeSum[rootNode] = nodeSum[childNode];
                onChild++;
            }
            while (onChild < subtreeSize) {
                int childNode = nodesInSubtree.get(onChild);
                if (childNode != rootNode) {
                    totalResult = modSumMultiply(totalResult, nodeSum[rootNode], nodeSum[childNode], topDepth);
                    nodeSum[rootNode] = modSum(nodeSum[rootNode], nodeSum[childNode]);
                }
                onChild++;
            }
            return;
        }

        int midDepth = topDepth + height/2;
        HashMap<Integer, ArrayList<Integer>> parentList = new HashMap<Integer, ArrayList<Integer>>();
        ArrayList<Integer> nodesTooHigh = new ArrayList<Integer>();

        for (int node : nodesInSubtree) {
            if (nodeDepth[node] < midDepth) {
                nodesTooHigh.add(node);
                continue;
            }
            
            int parentNode = getParentAtDepth(node, midDepth);
            parentList.computeIfAbsent(parentNode, k -> new ArrayList<Integer>()).add(node);
        }

        for (int pnode : parentList.keySet()) {
            ArrayList<Integer> eachChild = parentList.get(pnode);
            computeSubtreeResult(eachChild, midDepth, bottomDepth, pnode);
            nodesTooHigh.add(pnode);                
        }

        computeSubtreeResult(nodesTooHigh, topDepth, midDepth, rootNode);            
    }
    
    static final long computeFullDepthResult(ArrayList<Integer> nodeList, int nodeCount, long sumOfAllNodeValues) {
        long total = 0;
        for (int k = 0; k < nodeCount; k++) {
            int node = nodeList.get(k);
            int depth = nodeDepth[node];
            total = modSumMultiply(total, depth, node, (sumOfAllNodeValues - node));                
        }
        return total;
    }
    
    static final long combineResults(long totalEverything, long totalLCAOnly) {
        long temp = 2 * totalLCAOnly;
        if (temp >= MODMAX) temp %= MODMAX;
        temp = totalEverything - temp;
        if (temp < 0) temp += MODMAX;
        return temp;
    }
    
    static final long modSum(long accumulator, long term1) {
        accumulator += term1;
        if (accumulator >= MODMAX) accumulator %= MODMAX;
        return accumulator;
    }
    
    static final long modSumMultiply(long accumulator, long term1, long term2, long term3) {
        long temp = (term1 * term2) % MODMAX;    
        temp = (temp * term3) % MODMAX;
        accumulator += temp;
        if (accumulator >= MODMAX) accumulator %= MODMAX;
        return accumulator;
    }
    
    static final int getParentAtDepth(int node, int targetDepth) {
        int onDepth = nodeDepth[node];
        while (onDepth > targetDepth) {
            int diff = onDepth - targetDepth;
            int diff2 = Integer.highestOneBit(diff);
            int path = Integer.numberOfTrailingZeros(diff2);
            node = nodeLinks[node][path];
            onDepth -= diff2;
        }
        return node;
    }

    static void processTree(int nodeCount) {
        int maxSize = nodeCount + 1;
        
        nodeDepth = new int[maxSize];
        nodeLinks = new int[maxSize][18];
        
        ArrayDeque<Integer> queue = new ArrayDeque<>(maxSize);
        boolean[] isParent = new boolean[maxSize];
        
        queue.add(1);
        isParent[0] = true;
        isParent[1] = true;
        int[] nodePath = new int[maxSize];

        while (!queue.isEmpty()) {
            int onNode = queue.removeLast();
            int depth = nodeDepth[onNode];
            nodePath[depth] = onNode;

            int[] links = nodeLinks[onNode];
            int powerValue = 1;
            int linkNum = 0;
            while (true) {
                int index = depth - powerValue;
                if (index < 0) break;
                links[linkNum++] = nodePath[index];
                powerValue <<= 1;
            }
            
            for (int childNode : edges.get(onNode)) {
                if (!isParent[childNode]) {
                    isParent[childNode] = true;
                    nodeDepth[childNode] = depth + 1;
                    queue.addLast(childNode);
                }
            }
        }
    }

    static class Reader {
        final private int BUFFER_SIZE = 1 << 16;
        private DataInputStream din;
        private byte[] buffer;
        private int bufferPointer, bytesRead;

        public Reader() {
            din = new DataInputStream(System.in);
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public int nextInt() {
            int ret = 0;
            byte c = read();
            while (c <= ' ') c = read();

            boolean neg = (c == '-');
            if (neg) c = read();

            do {
                ret = ret * 10 + (c - '0');
            } while ((c = read()) >= '0' && c <= '9');

            return neg ? -ret : ret;
        }

        private void fillBuffer() {
            try {
                bytesRead = din.read(buffer, bufferPointer = 0, BUFFER_SIZE);
            } catch (Exception ex) {}
            if (bytesRead == -1) buffer[0] = -1;
        }

        private byte read() {
            if (bufferPointer == bytesRead)
                fillBuffer();
            return buffer[bufferPointer++];
        }
    }
}
