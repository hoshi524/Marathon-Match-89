import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MazeFixing {

	private static final int MAX_TIME = 9500;
	private static final Cell cell[] = new Cell[] { Cell.L, Cell.R, Cell.S };
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	private int W, H, WH, F, dir[], startPos[], startDir[], notN[];
	private Cell init[];

	public String[] improve(String[] maze, int F) {
		H = maze.length;
		W = maze[0].length();
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
		now.calc();
		int score = 0;
		Cell best[] = Arrays.copyOf(init, WH);
		int pos[] = new int[notN.length], pi;
		int dpos[] = new int[notN.length], f;
		while (true) {
			for (int turn = 0; turn < 5; ++turn) {
				f = pi = 0;
				for (int j : notN) {
					if (now.m[j] != init[j]) dpos[f++] = j;
					if (now.m[j] != Cell.E && now.b[j] > 0) pos[pi++] = j;
				}
				int value = now.value(f);
				Map<Integer, Cell> next = null;
				for (int i = 0; i < 0x2f; ++i) {
					Map<Integer, Cell> map = new HashMap<>();
					if (f > 0 && rnd.next(F) < f) {
						int a = dpos[rnd.next(f)];
						map.put(a, init[a]);
					}
					map.put(pos[rnd.next(pi)], cell[rnd.next(cell.length)]);
					int tmp = now.value(map, f);
					if (value < tmp) {
						value = tmp;
						next = map;
					}
				}
				if (next != null) {
					for (Entry<Integer, Cell> entry : next.entrySet()) {
						now.m[entry.getKey()] = entry.getValue();
					}
					now.calc();
					if (score < now.ac) {
						score = now.ac;
						System.arraycopy(now.m, 0, best, 0, WH);
					}
				}
			}
			if (System.currentTimeMillis() > endTime) {
				return toAnswer(best);
			}
		}
	}

	private final class State {
		int ac, bc;
		Cell m[] = new Cell[WH], tmp[] = new Cell[WH];
		int a[] = new int[WH], b[] = new int[WH], start[][] = new int[WH][128];
		int si[] = new int[WH], path[] = new int[WH];
		boolean used[] = new boolean[WH];
		int[] delA = new int[WH], delB = new int[WH];
		int[] addA = new int[WH], addB = new int[WH];
		Set<Integer> startSet = new HashSet<>();

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
			ac = bc = 0;
			for (int p : notN) {
				if (b[p] > 0) {
					++bc;
					if (a[p] > 0) ++ac;
				}
			}
		}

		int value(Map<Integer, Cell> map, int f) {
			startSet.clear();
			System.arraycopy(m, 0, tmp, 0, WH);
			for (Entry<Integer, Cell> entry : map.entrySet()) {
				int p = entry.getKey();
				Cell c = entry.getValue();
				for (int i = 0, size = si[p]; i < size; ++i) {
					startSet.add(start[p][i]);
				}
				tmp[p] = c;
			}
			Arrays.fill(delA, 0);
			Arrays.fill(delB, 0);
			Arrays.fill(addA, 0);
			Arrays.fill(addB, 0);
			for (Integer i : startSet) {
				dfs(-1, delA, path, 0, m, startPos[i], startDir[i], used, delB);
				dfs(-1, addA, path, 0, tmp, startPos[i], startDir[i], used, addB);
			}
			int ac = 0, bc = 0;
			for (int p : notN) {
				if (b[p] + addB[p] > delB[p]) {
					++bc;
					if (a[p] + addA[p] > delA[p]) ++ac;
				}
			}
			return value(f, ac, bc);
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
				for (int x : dir) {
					dfs(s, a, path, pi, m, p + x, x, used, b);
				}
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

		int value(int f) {
			return value(f, ac, bc);
		}

		int value(int f, int ac, int bc) {
			return (bc << 3) * (F - f) + ac * f;
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
