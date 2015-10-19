#define LOCAL

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cmath>
#include <climits>
#include <cfloat>
#include <ctime>
#include <cassert>
#include <map>
#include <utility>
#include <set>
#include <iostream>
#include <memory>
#include <string>
#include <vector>
#include <algorithm>
#include <functional>
#include <sstream>
#include <complex>
#include <stack>
#include <queue>
#include <numeric>
#include <list>
#include <iomanip>
#include <fstream>
#include <bitset>

using namespace std;

typedef long long ll;

const int MAX_TIME = 9500;

class MazeFixing {
public:

	int W, H, WH, F;
	vector<int> dir, startPos, startDir, notN, init;

	vector<string> improve(vector<string> maze, int F_){
		H = maze.size();
		W = maze[0].size();
		WH = W * H;
		F = F_;
		vector<string> res;
		return res;
	}
};

#ifdef LOCAL
int main() {
	int H, F;
	cin>>H;
	vector<string> maze(H);
	for (int i=0;i<H;++i) {
		string s;
		cin>>s;
		maze.push_back(s);
	}

	vector<string> res = MazeFixing().improve(maze, F);
	cout<<res.size()<<endl;
	for(int i=0;i<res.size();++i){
		cout<<res[i]<<endl;
	}
}
#endif

