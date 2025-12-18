#include <bits/stdc++.h>
using namespace std;

// Fast reader
struct FastInput {
    static const int BUFSIZE = 1<<20;
    int idx, size;
    char buf[BUFSIZE];
    FastInput(): idx(0), size(0) {}
    inline char read() {
        if (idx >= size) {
            size = fread(buf,1,BUFSIZE,stdin);
            idx = 0;
            if (!size) return 0;
        }
        return buf[idx++];
    }
    int nextInt() {
        char c; int sign = 1; int x = 0;
        do { c = read(); if (!c) return INT_MIN; } while (c!='-' && (c<'0' || c>'9'));
        if (c == '-') { sign = -1; c = read(); }
        while (c >= '0' && c <= '9') {
            x = x*10 + (c - '0');
            c = read();
        }
        return x*sign;
    }
} In;

// splitmix64 for mixing
static inline uint64_t splitmix64(uint64_t x) {
    x += 0x9e3779b97f4a7c15ULL;
    x = (x ^ (x >> 30)) * 0xbf58476d1ce4e5b9ULL;
    x = (x ^ (x >> 27)) * 0x94d049bb133111ebULL;
    x = x ^ (x >> 31);
    return x;
}

// Combine sorted child-hashes into single hash
static inline uint64_t combine_children(const vector<uint64_t>& ch) {
    // start from prime
    uint64_t h = 0x9ddfea08eb382d69ULL;
    // incorporate size
    h = splitmix64(h ^ (uint64_t)ch.size());
    for (uint64_t x : ch) {
        // mix child hash
        uint64_t m = splitmix64(x ^ 0x9e3779b97f4a7c15ULL);
        h ^= m + 0x9e3779b97f4a7c15ULL + (h<<6) + (h>>2);
    }
    // final scramble
    return splitmix64(h);
}

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n = In.nextInt();
    if (n == INT_MIN) return 0;
    int K = In.nextInt();

    vector<vector<int>> g(n);
    for (int i = 0; i < n-1; ++i) {
        int u = In.nextInt(), v = In.nextInt();
        --u; --v;
        g[u].push_back(v);
        g[v].push_back(u);
    }

    // buffers reused per center
    vector<char> inBall(n, 0);
    vector<int> dist(n, 0);
    vector<int> nodes; nodes.reserve(n);
    vector<int> pos(n, -1); // mapping original->compact index
    vector<vector<int>> subAdj;
    vector<int> deg;
    vector<int> leafq;
    vector<char> removedFlag;
    vector<int> parent;
    vector<int> order;
    vector<uint64_t> nodeHash;
    unordered_set<uint64_t> seen;
    seen.reserve(n*2);

    for (int center = 0; center < n; ++center) {
        nodes.clear();

        // BFS limited by K
        deque<int> dq;
        dq.push_back(center);
        inBall[center] = 1;
        dist[center] = 0;
        nodes.push_back(center);

        while (!dq.empty()) {
            int u = dq.front(); dq.pop_front();
            int du = dist[u];
            if (du == K) continue;
            for (int v : g[u]) {
                if (!inBall[v]) {
                    inBall[v] = 1;
                    dist[v] = du + 1;
                    dq.push_back(v);
                    nodes.push_back(v);
                }
            }
        }

        int m = nodes.size();
        if (m == 1) {
            // single node -> canonical hash
            uint64_t h = combine_children(vector<uint64_t>()); // empty children vector
            uint64_t sig = h ^ (h<<1);
            seen.insert(sig);
            // clear inBall marks
            inBall[center] = 0;
            continue;
        }

        // build pos mapping
        for (int i = 0; i < m; ++i) pos[nodes[i]] = i;

        // build induced adjacency
        subAdj.assign(m, vector<int>());
        for (int i = 0; i < m; ++i) {
            int u = nodes[i];
            for (int v : g[u]) {
                if (pos[v] != -1) subAdj[i].push_back(pos[v]);
            }
        }

        // find centers by peeling leaves
        deg.assign(m,0);
        leafq.clear();
        for (int i = 0; i < m; ++i) {
            deg[i] = subAdj[i].size();
            if (deg[i] <= 1) leafq.push_back(i);
        }
        int removed = 0;
        removedFlag.assign(m, 0);
        deque<int> cur;
        for (int x : leafq) cur.push_back(x);
        while (m - removed > 2 && !cur.empty()) {
            int s = cur.size();
            deque<int> next;
            for (int t=0;t<s;++t) {
                int leaf = cur.front(); cur.pop_front();
                if (removedFlag[leaf]) continue;
                removedFlag[leaf] = 1;
                removed++;
                for (int nb : subAdj[leaf]) {
                    if (!removedFlag[nb]) {
                        deg[nb]--;
                        if (deg[nb]==1) next.push_back(nb);
                    }
                }
            }
            cur.swap(next);
        }
        vector<int> centers;
        for (int i = 0; i < m; ++i) if (!removedFlag[i]) centers.push_back(i);
        if (centers.empty()) centers.push_back(0);

        // prepare arrays for computing rooted hashes
        parent.assign(m, -2);
        order.clear();
        nodeHash.assign(m, 0);

        vector<uint64_t> center_hashes;
        center_hashes.reserve(centers.size());

        for (int cidx : centers) {
            // build parent & order via stack (iterative DFS/BFS)
            order.clear();
            // use stack to traverse and set parent
            deque<int> st;
            st.push_back(cidx);
            parent[cidx] = -1;
            order.push_back(cidx);
            for (size_t ii = 0; ii < order.size(); ++ii) {
                int u = order[ii];
                for (int v : subAdj[u]) {
                    if (parent[v] == -2) { // not visited at all in this center run
                        parent[v] = u;
                        order.push_back(v);
                    }
                }
            }
            // note: parent array has previous runs' values for nodes not visited; we set only visited ones to proper, so for correctness reset visited afterwards
            // compute nodeHash in reverse order
            for (int ii = (int)order.size()-1; ii >= 0; --ii) {
                int u = order[ii];
                vector<uint64_t> ch;
                ch.reserve(subAdj[u].size());
                for (int v : subAdj[u]) {
                    if (parent[v] == u) ch.push_back(nodeHash[v]);
                }
                sort(ch.begin(), ch.end());
                uint64_t h = combine_children(ch);
                // incorporate a delimiter to indicate root layer depth implicitly
                nodeHash[u] = h ^ (uint64_t) (ch.size() * 0x9e3779b97f4a7c15ULL);
            }
            center_hashes.push_back(nodeHash[cidx]);
            // reset parent for nodes in order to -2 to reuse for next center
            for (int u : order) parent[u] = -2;
        }

        // combine center_hashes into single signature (order-independent)
        uint64_t sig;
        if (center_hashes.size() == 1) {
            uint64_t h = center_hashes[0];
            sig = h ^ (h << 1);
        } else {
            uint64_t a = center_hashes[0], b = center_hashes[1];
            if (a > b) swap(a,b);
            // mix pair into single uint64
            uint64_t h = splitmix64(a) ^ (splitmix64(b) + 0x9e3779b97f4a7c15ULL + (splitmix64(a)<<6) + (splitmix64(a)>>2));
            sig = splitmix64(h);
        }
        seen.insert(sig);

        // clear pos and inBall flags for nodes
        for (int u : nodes) {
            pos[u] = -1;
            inBall[u] = 0;
        }
    }

    cout << seen.size() << "\n";
    return 0;
}
