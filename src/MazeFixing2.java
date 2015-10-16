import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class MazeFixing2 {

	private static final int MAX_TIME = 9500;
	private static final Cell cell[] = new Cell[] { Cell.L, Cell.R, Cell.S };
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	private int W, H, WH, F, dir[], startPos[], startDir[], notN[];
	private Cell init[];

	public String[] improve(String[] maze, int F) {
		H = maze.length;
		W = maze[0].length() - 1;
		WH = W * H;
		this.F = F;
		dir = new int[] { 1, -1, W, -W };
		Cell m[] = new Cell[WH];
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j < W; ++j) {
				m[getPos(i, j)] = Cell.get(maze[i].charAt(j));
			}
		}
		init = Arrays.copyOf(m, m.length);
		{
			List<Integer> spos = new ArrayList<>();
			List<Integer> sdir = new ArrayList<>();
			List<Integer> cell = new ArrayList<>();
			for (int i = 0; i < WH; ++i) {
				if (m[i] != Cell.N) {
					cell.add(i);
					for (int d : dir) {
						int n = i + d;
						if (m[n] == Cell.N) {
							spos.add(i);
							sdir.add(-d);
						}
					}
				}
			}
			startPos = toArray(spos);
			startDir = toArray(sdir);
			notN = toArray(cell);
		}
		XorShift rnd = new XorShift();
		State now = new State(m);
		int score = 0;
		Cell best[] = Arrays.copyOf(init, WH);
		int pos[] = new int[notN.length], pi = 0;
		int dpos[] = new int[notN.length], f;
		{
			for (int j : notN) {
				if (now.m[j] == Cell.U) {
					pos[pi++] = j;
				}
			}
			for (int i = 0; i < F && pi > 0; ++i) {
				int index = rnd.next(pi), p = pos[index];
				now.m[p] = Cell.S;
				pos[index] = pos[--pi];
			}
		}
		now.calc();
		HashMap<Integer, Cell> next = new HashMap<>(), map = new HashMap<>();
		while (true) {
			f = pi = 0;
			for (int j : notN) {
				if (now.m[j] != init[j]) dpos[f++] = j;
				if (now.m[j] != Cell.E && now.b[j] > 0) pos[pi++] = j;
			}
			int value = 0;
			for (int i = 0; i < 0x3f; ++i) {
				map.clear();
				if (rnd.next(F) < f) {
					int a = dpos[rnd.next(f)];
					map.put(a, init[a]);
				}
				map.put(pos[rnd.next(pi)], cell[rnd.next(cell.length)]);
				int tmp = now.value(map, f);
				if (value < tmp) {
					value = tmp;
					next.clear();
					next.putAll(map);
				}
			}
			for (Entry<Integer, Cell> entry : next.entrySet()) {
				now.m[entry.getKey()] = entry.getValue();
			}
			now.calc();
			if (score < now.ac) {
				score = now.ac;
				System.arraycopy(now.m, 0, best, 0, WH);
			}
			if (System.currentTimeMillis() > endTime) {
				return toAnswer(best);
			}
		}
	}

	private final class State {
		int ac;
		Cell m[] = new Cell[WH];
		int a[] = new int[WH], b[] = new int[WH], start[][] = new int[WH][64];
		int si[] = new int[WH], path[] = new int[WH];
		boolean used[] = new boolean[WH];

		State(Cell m[]) {
			System.arraycopy(m, 0, this.m, 0, WH);
		}

		void calc() {
			Arrays.fill(a, 0);
			Arrays.fill(b, 0);
			Arrays.fill(si, 0);
			for (int i = 0; i < startPos.length; ++i) {
				dfs(i, a, path, 0, m, startPos[i], startDir[i], used, b);
			}
			ac = 0;
			for (int p : notN) {
				if (a[p] > 0) ++ac;
			}
		}

		int value(HashMap<Integer, Cell> map, int f) {
			boolean ud[] = new boolean[startPos.length];
			Cell tmp[] = Arrays.copyOf(m, WH);
			int delA[] = new int[WH];
			int delB[] = new int[WH];
			int addA[] = new int[WH];
			int addB[] = new int[WH];
			int buf[] = new int[0xff], bi = 0;
			for (Entry<Integer, Cell> entry : map.entrySet()) {
				int p = entry.getKey();
				Cell c = entry.getValue();
				if (m[p] != c) {
					for (int i = 0, size = si[p]; i < size; ++i) {
						int x = start[p][i];
						if (!ud[x]) {
							ud[x] = true;
							buf[bi++] = x;
						}
					}
					tmp[p] = c;
				}
			}
			for (int i = 0; i < bi; ++i) {
				int x = buf[i];
				dfs(-1, delA, path, 0, m, startPos[x], startDir[x], used, delB);
				dfs(-1, addA, path, 0, tmp, startPos[x], startDir[x], used, addB);
			}
			int ac = 0, bc = 0;
			for (int p : notN) {
				if (b[p] + addB[p] > delB[p]) {
					++bc;
					if (a[p] + addA[p] > delA[p]) ++ac;
				}
			}
			// value
			return (bc << 3) * (F - f) + ac * f;
		}

		void dfs(int s, int a[], int path[], int pi, Cell m[], int p, int d, boolean used[], int b[]) {
			if (used[p]) return;
			if (m[p] == Cell.N) {
				for (int i = 0; i < pi; ++i)
					++a[path[i]];
				return;
			}
			path[pi++] = p;
			used[p] = true;
			++b[p];
			if (m[p] == Cell.E) {
				dfs(s, a, path, pi, m, p + 1, 1, used, b);
				dfs(s, a, path, pi, m, p - 1, -1, used, b);
				dfs(s, a, path, pi, m, p + W, W, used, b);
				dfs(s, a, path, pi, m, p - W, -W, used, b);
			} else {
				if (s != -1 && (si[p] == 0 || start[p][si[p] - 1] != s)) start[p][si[p]++] = s;
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
				dfs(s, a, path, pi, m, p + d, d, used, b);
			}
			used[p] = false;
		}
	}

	private String[] toAnswer(Cell m[]) {
		ArrayList<String> res = new ArrayList<>();
		for (int i : notN) {
			if (m[i] != init[i]) {
				res.add(getRow(i) + " " + getCol(i) + " " + m[i]);
			}
		}
		return res.toArray(new String[0]);
	}

	private static enum Cell {
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

	private int getPos(int r, int c) {
		return r * W + c;
	}

	private int getRow(int p) {
		return p / W;
	}

	private int getCol(int p) {
		return p % W;
	}

	private int[] toArray(List<Integer> list) {
		int res[] = new int[list.size()];
		for (int i = 0; i < res.length; ++i) {
			res[i] = list.get(i);
		}
		return res;
	}

	private static final class XorShift {
		int x = 123456789;
		int y = 362436069;
		int z = 521288629;
		int w = 88675123;

		int next(final int n) {
			final int t = x ^ (x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ (w >>> 19)) ^ (t ^ (t >>> 8));
			final int r = w % n;
			return r < 0 ? r + n : r;
		}
	}

	private void debug(Object... o) {
		System.out.println(Arrays.deepToString(o));
	}
}
