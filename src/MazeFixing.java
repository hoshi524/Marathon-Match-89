import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MazeFixing {
	int W, H, WH, dir[];

	public String[] improve(String[] maze, int F) {
		H = maze.length;
		W = maze[0].length();
		WH = W * H;
		dir = new int[] { 1, -1, W, -W };
		Cell m[] = new Cell[WH];
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j < W; ++j) {
				m[getPos(i, j)] = Cell.get(maze[i].charAt(j));
			}
		}
		int start[];
		{
			Set<Integer> pos = new HashSet<>();
			for (int i = 0; i < WH; ++i) {
				if (m[i] != Cell.N) {
					for (int d : dir) {
						int n = i + d;
						if (m[n] == Cell.N) pos.add(n);
					}
				}
			}
			start = toArray(new ArrayList<Integer>(pos));
		}
		int value[] = new int[WH];
		{
			boolean used[] = new boolean[WH];
			for (int p : start) {
				for (int d : dir) {
					int n = p + d;
					if (n >= 0 && n < WH && m[n] != Cell.N) {
						Arrays.fill(used, false);
						dfs(value, m, n, d, used);
					}
				}
			}
		}
		System.err.println(count(value, m));
		ArrayList<Answer> ans = new ArrayList<>();
		XorShift rnd = new XorShift();
		Cell[] v = new Cell[] { Cell.R, Cell.L, Cell.S };
		for (int i = 0; i < WH; ++i) {
			if (value[i] > 0 && m[i] == Cell.U && ans.size() + 1 < F) {
				Cell c = v[Math.abs(rnd.next()) % v.length];
				// ans.add(new Answer(i, c));
				m[i] = c;
			}
		}
		return toAnswer(ans);
	}

	class Answer {
		final int p;
		final Cell c;

		Answer(int p, Cell c) {
			this.p = p;
			this.c = c;
		}

		public String toString() {
			return getRow(p) + " " + getCol(p) + " " + c.name();
		}
	}

	void dfs(int v[], Cell m[], int p, int d, boolean used[]) {
		if (m[p] == Cell.N || used[p]) return;
		used[p] = true;
		++v[p];
		if (m[p] == Cell.R) {
			if (d == 1) d = W;
			else if (d == -1) d = -W;
			else if (d == W) d = -1;
			else if (d == -W) d = 1;
			dfs(v, m, p + d, d, used);
		} else if (m[p] == Cell.L) {
			if (d == 1) d = -W;
			else if (d == -1) d = W;
			else if (d == W) d = 1;
			else if (d == -W) d = -1;
			dfs(v, m, p + d, d, used);
		} else if (m[p] == Cell.S) {
			dfs(v, m, p + d, d, used);
		} else if (m[p] == Cell.E) {
			for (int a : dir) {
				if (a != -d && m[p + a] != Cell.N) {
					dfs(v, m, p + a, a, used);
				}
			}
		}
	}

	enum Cell {
		N, R, L, U, S, E;

		static Cell get(char c) {
			if (c == 'R') return R;
			else if (c == 'L') return L;
			else if (c == 'U') return U;
			else if (c == 'S') return S;
			else if (c == 'E') return E;
			return N;
		}
	}

	int getPos(int r, int c) {
		return r * W + c;
	}

	int getRow(int p) {
		return p / W;
	}

	int getCol(int p) {
		return p % W;
	}

	int[] toArray(List<Integer> list) {
		int res[] = new int[list.size()];
		for (int i = 0; i < res.length; ++i) {
			res[i] = list.get(i);
		}
		return res;
	}

	String[] toAnswer(List<Answer> list) {
		String[] res = new String[list.size()];
		for (int i = 0; i < res.length; ++i) {
			res[i] = list.get(i).toString();
		}
		return res;
	}

	int count(int v[], Cell m[]) {
		int res = 0;
		for (int i = 0; i < WH; ++i) {
			if (v[i] > 0 && m[i] != Cell.N) ++res;
		}
		return res;
	}

	void print(Cell m[]) {
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j < W; ++j) {
				System.out.print(m[getPos(i, j)]);
			}
			System.out.println();
		}
	}

	class XorShift {
		private long x, y, z, w;

		{
			x = 123456789;
			y = 362436069;
			z = 521288629;
			w = 88675123;
		}

		int next() {
			long t = (x ^ x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ w >>> 19 ^ t ^ t >>> 8);
			return (int) w;
		}

		long nextLong() {
			long t = (x ^ x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ w >>> 19 ^ t ^ t >>> 8);
			return w;
		}
	}
}
