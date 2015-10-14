import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MazeFixing2 {

	private static final int MAX_TIME = 9500;
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	int W, H, WH, dir[], start[], notN[];
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
			List<Integer> cell = new ArrayList<>();
			for (int i = 0; i < WH; ++i) {
				if (m[i] != Cell.N) {
					cell.add(i);
					for (int d : dir) {
						int n = i + d;
						if (m[n] == Cell.N) pos.add(n);
					}
				}
			}
			start = toArray(new ArrayList<Integer>(pos));
			notN = toArray(cell);
		}
		State best = new State(init);
		XorShift rnd = new XorShift();
		State start = new State(init);
		start.calc();
		int pos[] = new int[notN.length], pi;
		int delpos[] = new int[notN.length], dpi;
		Cell cell[] = new Cell[] { Cell.L, Cell.R, Cell.S };
		while (true) {
			State now = new State(start);
			for (int f = 0; f < F && now.ac < notN.length; ++f) {
				State fs = new State(now);
				pi = 0;
				for (int j : notN)
					if (fs.m[j] != Cell.E && fs.b[j]) pos[pi++] = j;
				for (int i = 0; i < 9 && pi > 0; ++i) {
					int index = Math.abs(rnd.next()) % pi;
					int p = pos[index];
					pos[index] = pos[--pi];
					for (Cell c : cell) {
						State tmp = new State(fs);
						tmp.m[p] = c;
						tmp.calc();
						if (now.value() < tmp.value()) now = tmp;
					}
				}
			}
			if (best.value() < now.value()) best = now;
			if (System.currentTimeMillis() + 3000 > endTime) break;
		}
		while (true) {
			State now = new State(best);
			int f = now.calcF(init);
			dpi = 0;
			for (int j : notN)
				if (init[j] != now.m[j]) delpos[dpi++] = j;
			pi = 0;
			for (int j : notN)
				if (now.m[j] != Cell.E && now.b[j]) pos[pi++] = j;
			for (int i = 0; i < 0xff && pi > 0; ++i) {
				int dp = delpos[Math.abs(rnd.next()) % dpi];
				int index = Math.abs(rnd.next()) % pi;
				int p = pos[index];
				pos[index] = pos[--pi];
				for (Cell c : cell) {
					State tmp = new State(now);
					if (f == F) tmp.m[dp] = init[dp];
					tmp.m[p] = c;
					tmp.calc();
					if (best.ac < tmp.ac) {
						best = tmp;
					}
				}
			}
			if (System.currentTimeMillis() > endTime) {
				// System.err.println(best.ac + " / " + best.bc + " / " + notN.length);
				return best.toString(init);
			}
		}
	}

	class State {
		int ac, bc;
		Cell m[];
		boolean a[], b[];

		State(Cell m[]) {
			this.m = m;
		}

		State(State s) {
			m = Arrays.copyOf(s.m, WH);
			a = s.a;
			b = s.b;
			ac = s.ac;
			bc = s.bc;
		}

		void calc() {
			a = new boolean[WH];
			b = new boolean[WH];
			boolean used[] = new boolean[WH];
			int path[] = new int[WH];
			for (int p : start) {
				for (int d : dir) {
					int n = p + d;
					if (n >= 0 && n < WH && m[n] != Cell.N) {
						dfs(a, path, 0, m, n, d, used, b);
					}
				}
			}
			ac = bc = 0;
			for (int p : notN) {
				if (b[p]) {
					++bc;
					if (a[p]) ++ac;
				}
			}
		}

		int calcF(Cell init[]) {
			int res = 0;
			for (int p : notN)
				if (init[p] != m[p]) ++res;
			return res;
		}

		int value() {
			return (bc << 10) + ac;
		}

		String[] toString(Cell init[]) {
			ArrayList<String> res = new ArrayList<>();
			for (int i : notN) {
				if (m[i] != init[i]) {
					res.add(getRow(i) + " " + getCol(i) + " " + m[i]);
				}
			}
			return res.toArray(new String[0]);
		}
	}

	void dfs(boolean a[], int path[], int pi, Cell m[], int p, int d, boolean used[], boolean b[]) {
		if (used[p]) return;
		if (m[p] == Cell.N) {
			for (int i = 0; i < pi; ++i)
				a[path[i]] = true;
			return;
		}
		path[pi++] = p;
		used[p] = b[p] = true;
		if (m[p] == Cell.E) {
			for (int x : dir) {
				dfs(a, path, pi, m, p + x, x, used, b);
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
			dfs(a, path, pi, m, p + d, d, used, b);
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
