import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MazeFixing {

	private static final int MAX_TIME = 9500;
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	int W, H, WH, dir[], start[];
	Cell init[];

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
		init = Arrays.copyOf(m, m.length);
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
		State best = new State(init);
		XorShift rnd = new XorShift();
		State start = new State(init);
		start.calcScore();
		int pos[] = new int[WH], pi;
		Cell cell[] = new Cell[] { Cell.L, Cell.R, Cell.S };
		while (System.currentTimeMillis() < endTime) {
			State now = new State(start);
			for (int f = 0; f < F; ++f) {
				State fs = new State(now);
				for (int i = 0; i < 100; ++i) {
					State tmp = new State(fs);
					pi = 0;
					for (int j = 0; j < WH; ++j)
						if (tmp.count[j] > 0) pos[pi++] = j;
					int p = pos[Math.abs(rnd.next()) % pi];
					Cell c = cell[Math.abs(rnd.next()) % cell.length];
					while (c == tmp.m[p])
						c = cell[Math.abs(rnd.next()) % cell.length];
					if (init[p] != c) ++tmp.f;
					tmp.m[p] = c;
					tmp.calcScore();
					if (now.score < tmp.score) {
						now = tmp;
					}
				}
			}
			if (best.score < now.score) {
				best = now;
			}
		}
		return best.toString(init);
	}

	class State {
		int score, f, value[], count[];
		Cell m[];

		State(Cell m[]) {
			this.m = m;
		}

		State(State s) {
			m = Arrays.copyOf(s.m, WH);
			score = s.score;
			f = s.f;
			value = s.value;
			count = s.count;
		}

		void calcScore() {
			value = new int[WH];
			count = new int[WH];
			boolean used[] = new boolean[WH];
			int path[] = new int[WH];
			for (int p : start) {
				for (int d : dir) {
					int n = p + d;
					if (n >= 0 && n < WH && m[n] != Cell.N) {
						dfs(value, path, 0, m, n, d, used, count);
					}
				}
			}
			score = count(value, m);
		}

		String[] toString(Cell init[]) {
			ArrayList<String> res = new ArrayList<>();
			for (int i = 0; i < WH; ++i) {
				if (m[i] != init[i]) {
					res.add(getRow(i) + " " + getCol(i) + " " + m[i]);
				}
			}
			return res.toArray(new String[0]);
		}
	}

	void dfs(int value[], int path[], int pi, Cell m[], int p, int d, boolean used[], int count[]) {
		if (used[p]) return;
		if (m[p] == Cell.N) {
			for (int i = 0; i < pi; ++i)
				++value[path[i]];
			return;
		}
		++count[p];
		path[pi++] = p;
		used[p] = true;
		if (m[p] == Cell.E) {
			for (int a : dir) {
				dfs(value, path, pi, m, p + a, a, used, count);
			}
		} else {
			if (m[p] == Cell.R) {
				if (d == 1) d = W;
				else if (d == -1) d = -W;
				else if (d == W) d = -1;
				else if (d == -W) d = 1;
			} else if (m[p] == Cell.L) {
				if (d == 1) d = -W;
				else if (d == -1) d = W;
				else if (d == W) d = 1;
				else if (d == -W) d = -1;
			} else if (m[p] == Cell.U) {
				d = -d;
			}
			dfs(value, path, pi, m, p + d, d, used, count);
		}
		used[p] = false;
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

	void print(int v[]) {
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j < W; ++j) {
				System.out.print(v[getPos(i, j)] > 0 ? '*' : ' ');
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
