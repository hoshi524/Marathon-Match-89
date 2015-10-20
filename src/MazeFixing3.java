import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class MazeFixing3 {

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
			for (int j = 0; j + 1 < maze[i].length(); ++j) {
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
		int pos[] = new int[notN.length << 1], pi = 0;
		int dpos[] = new int[notN.length], f = 0;
		for (int j : notN) {
			if (now.m[j] == Cell.U && f < F) {
				now.m[j] = Cell.S;
				++f;
			}
		}
		now.calc();
		HashMap<Integer, Cell> next = new HashMap<>(), map = new HashMap<>();
		for (int turn = 0;; ++turn) {
			f = pi = 0;
			for (int j : notN) {
				if (now.m[j] != init[j]) {
					dpos[f++] = j;
					// if (init[j] != Cell.U) dpos[f++] = j;
				}
				if (now.m[j] != Cell.E && now.b[j] + now.a[j] > 0) {
					pos[pi++] = j;
					if (now.m[j] == Cell.U) pos[pi++] = j;
				}
			}
			int value = 0;
			for (int i = 0; i < 80; ++i) {
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
			value = now.update(next);
			if (score < value) {
				score = value;
				System.arraycopy(now.m, 0, best, 0, WH);
			}
			if (System.currentTimeMillis() > endTime) {
				System.err.println("turn : " + turn);
				return toAnswer(best);
			}
		}
	}

	private final class State {
		Cell m[] = new Cell[WH];
		int a[] = new int[WH], b[] = new int[WH];
		int sa[][] = new int[startPos.length][WH], sb[][] = new int[startPos.length][WH];
		int start[][] = new int[WH][64], si[] = new int[WH];
		boolean use[] = new boolean[WH];

		State(Cell m[]) {
			System.arraycopy(m, 0, this.m, 0, WH);
			Arrays.fill(use, true);
		}

		void calc() {
			Arrays.fill(a, 0);
			Arrays.fill(b, 0);
			Arrays.fill(si, 0);
			for (int i = 0; i < startPos.length; ++i) {
				Arrays.fill(sa[i], 0);
				Arrays.fill(sb[i], 0);
				dfs(i, sa[i], m, startPos[i], startDir[i], sb[i]);
				for (int p : notN) {
					a[p] += sa[i][p];
					b[p] += sb[i][p];
				}
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
				for (int p : notN) {
					delA[p] += sa[x][p];
					delB[p] += sb[x][p];
				}
				dfs(addA, tmp, startPos[x], startDir[x], addB);
			}
			int ac = 0, bc = 0;
			for (int p : notN) {
				if (a[p] + addA[p] > delA[p]) ++ac;
				else if (b[p] + addB[p] > delB[p]) ++bc;
			}
			// value
			return ((bc + ac) << 5) * (F - f) + ac * f;
		}

		int update(HashMap<Integer, Cell> map) {
			boolean ud[] = new boolean[startPos.length];
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
				}
			}
			for (int i = 0; i < bi; ++i) {
				int x = buf[i];
				delete(x, a, m, startPos[x], startDir[x], b);
			}
			for (Entry<Integer, Cell> entry : map.entrySet()) {
				m[entry.getKey()] = entry.getValue();
			}
			for (int i = 0; i < bi; ++i) {
				int x = buf[i];
				Arrays.fill(sa[x], 0);
				Arrays.fill(sb[x], 0);
				dfs(x, sa[x], m, startPos[x], startDir[x], sb[x]);
				for (int p : notN) {
					a[p] += sa[x][p];
					b[p] += sb[x][p];
				}
			}
			int score = 0;
			for (int p : notN) {
				if (a[p] > 0) ++score;
			}
			return score;
		}

		boolean dfs(int s, int a[], Cell m[], int p, int d, int b[]) {
			if (m[p] == Cell.N) return true;
			boolean res = false;
			use[p] = false;
			if (m[p] == Cell.E) {
				if (use[p + 1]) res = dfs(s, a, m, p + 1, 1, b);
				if (use[p - 1]) res |= dfs(s, a, m, p - 1, -1, b);
				if (use[p + W]) res |= dfs(s, a, m, p + W, W, b);
				if (use[p - W]) res |= dfs(s, a, m, p - W, -W, b);
			} else {
				if (si[p] == 0 || start[p][si[p] - 1] != s) start[p][si[p]++] = s;
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
				if (use[p + d]) res = dfs(s, a, m, p + d, d, b);
			}
			use[p] = true;
			if (res) ++a[p];
			else ++b[p];
			return res;
		}

		boolean dfs(int a[], Cell m[], int p, int d, int b[]) {
			if (m[p] == Cell.N) return true;
			boolean res = false;
			use[p] = false;
			if (m[p] == Cell.E) {
				if (use[p + 1]) res = dfs(a, m, p + 1, 1, b);
				if (use[p - 1]) res |= dfs(a, m, p - 1, -1, b);
				if (use[p + W]) res |= dfs(a, m, p + W, W, b);
				if (use[p - W]) res |= dfs(a, m, p - W, -W, b);
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
				if (use[p + d]) res = dfs(a, m, p + d, d, b);
			}
			use[p] = true;
			if (res) ++a[p];
			else ++b[p];
			return res;
		}

		boolean delete(int s, int a[], Cell m[], int p, int d, int b[]) {
			if (m[p] == Cell.N) return true;
			boolean res = false;
			use[p] = false;
			if (m[p] == Cell.E) {
				if (use[p + 1]) res = delete(s, a, m, p + 1, 1, b);
				if (use[p - 1]) res |= delete(s, a, m, p - 1, -1, b);
				if (use[p + W]) res |= delete(s, a, m, p + W, W, b);
				if (use[p - W]) res |= delete(s, a, m, p - W, -W, b);
			} else {
				for (int i = 0; i < si[p]; ++i) {
					if (start[p][i] == s) {
						start[p][i] = start[p][--si[p]];
						break;
					}
				}
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
				if (use[p + d]) res = delete(s, a, m, p + d, d, b);
			}
			use[p] = true;
			if (res) --a[p];
			else --b[p];
			return res;
		}
	}

	private String[] toAnswer(Cell m[]) {
		ArrayList<String> res = new ArrayList<>();
		int buf[][] = new int[Cell.values().length][Cell.values().length], c = 0;
		for (int i : notN) {
			if (m[i] != init[i]) {
				res.add(getRow(i) + " " + getCol(i) + " " + m[i]);
				++c;
				++buf[init[i].ordinal()][m[i].ordinal()];
			}
		}
		if (false) {
			StringBuilder s = new StringBuilder();
			s.append("sum : " + c + "\n");
			for (Cell a : Cell.values()) {
				for (Cell b : Cell.values()) {
					if (a != Cell.N && b != Cell.N && a != Cell.E && b != Cell.U && b != Cell.E && a != b) s.append(a.name() + " -> "
							+ b.name() + " : " + buf[a.ordinal()][b.ordinal()] + "\n");
				}
			}
			System.err.print(s.toString());
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
