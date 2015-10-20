import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class MazeFixing3 {

	private static final int MAX_TIME = 9500;
	private static final Cell cell[] = new Cell[] { Cell.L, Cell.R, Cell.S };
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	private int W, WH, F, S[][];
	private Cell init[];

	public String[] improve(String[] maze, int F) {
		int H = maze.length;
		W = maze[0].length() - 1;
		WH = W * H;
		this.F = F;
		Cell m[] = new Cell[WH];
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j + 1 < maze[i].length() && getPos(i, j) < WH; ++j) {
				int p = getPos(i, j);
				m[p] = Cell.get(maze[i].charAt(j));
			}
		}
		init = Arrays.copyOf(m, m.length);
		{
			List<int[]> slist = new ArrayList<>();
			int dir[] = new int[] { 1, -1, W, -W };
			for (int i = 0; i < WH; ++i) {
				if (m[i] != Cell.N) {
					for (int d : dir) {
						int n = i + d;
						if (m[n] == Cell.N) slist.add(new int[] { i, -d });
					}
				}
			}
			S = toArray(slist);
		}
		XorShift rnd = new XorShift();
		State now = new State(m);
		int score = 0;
		Cell best[] = Arrays.copyOf(init, WH);
		int pos[] = new int[WH << 1], pi = 0;
		int dpos[] = new int[WH], f = 0;
		for (int p = 0; p < WH; ++p) {
			if (now.m[p] == Cell.U && f < F) {
				now.m[p] = Cell.S;
				++f;
			}
		}
		now.calc();
		HashMap<Integer, Cell> next = new HashMap<>(), map = new HashMap<>();
		for (int turn = 0;; ++turn) {
			f = pi = 0;
			for (int p = 0; p < WH; ++p) {
				if (now.m[p] == Cell.E || now.m[p] == Cell.N) continue;
				if (now.m[p] != init[p]) {
					dpos[f++] = p;
					// if (init[j] != Cell.U) dpos[f++] = j;
				}
				if (now.b[p] + now.a[p] > 0) {
					pos[pi++] = p;
					if (now.m[p] == Cell.U) pos[pi++] = p;
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
		int sa[][] = new int[S.length][WH], sb[][] = new int[S.length][WH];
		private static final int SSIZE = 50;
		int start[][] = new int[WH][SSIZE];
		boolean use[] = new boolean[WH];

		State(Cell m[]) {
			System.arraycopy(m, 0, this.m, 0, WH);
			Arrays.fill(use, true);
		}

		void calc() {
			Arrays.fill(a, 0);
			Arrays.fill(b, 0);
			for (int p = 0; p < WH; ++p)
				start[p][0] = 1;
			for (int i = 0; i < S.length; ++i) {
				Arrays.fill(sa[i], 0);
				Arrays.fill(sb[i], 0);
				dfs(i, sa[i], m, S[i][0], S[i][1], sb[i]);
				for (int p = 0; p < WH; ++p) {
					a[p] += sa[i][p];
					b[p] += sb[i][p];
				}
			}
		}

		int value(HashMap<Integer, Cell> map, int f) {
			boolean ud[] = new boolean[S.length];
			Cell tmp[] = Arrays.copyOf(m, WH);
			int tmpA[] = new int[WH];
			int tmpB[] = new int[WH];
			int buf[] = new int[0xff], bi = 0;
			for (Entry<Integer, Cell> entry : map.entrySet()) {
				int p = entry.getKey();
				Cell c = entry.getValue();
				if (m[p] != c) {
					for (int i = 1, size = start[p][0]; i < size; ++i) {
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
				for (int p = 0; p < WH; ++p) {
					tmpA[p] -= sa[x][p];
					tmpB[p] -= sb[x][p];
				}
				dfs(tmpA, tmp, S[x][0], S[x][1], tmpB);
			}
			int ac = 0, bc = 0;
			for (int p = 0; p < WH; ++p) {
				if (a[p] + tmpA[p] > 0) ++ac;
				else if (b[p] + tmpB[p] > 0) ++bc;
			}
			// value
			return ((bc + ac) << 5) * (F - f) + ac * f;
		}

		int update(HashMap<Integer, Cell> map) {
			boolean ud[] = new boolean[S.length];
			int buf[] = new int[0xff], bi = 0;
			for (Entry<Integer, Cell> entry : map.entrySet()) {
				int p = entry.getKey();
				Cell c = entry.getValue();
				if (m[p] != c) {
					for (int i = 1, size = start[p][0]; i < size; ++i) {
						int x = start[p][i];
						if (!ud[x]) {
							ud[x] = true;
							buf[bi++] = x;
						}
					}
					m[entry.getKey()] = entry.getValue();
				}
			}
			for (int i = 0; i < bi; ++i) {
				int x = buf[i];
				for (int p = 0; p < WH; ++p) {
					if (sa[x][p] > 0 || sb[x][p] > 0) {
						a[p] -= sa[x][p];
						b[p] -= sb[x][p];
						sa[x][p] = sb[x][p] = 0;
						for (int k = 1; k < start[p][0]; ++k) {
							if (start[p][k] == x) {
								start[p][k] = start[p][--start[p][0]];
								break;
							}
						}
					}
				}
				dfs(x, sa[x], m, S[x][0], S[x][1], sb[x]);
				for (int p = 0; p < WH; ++p) {
					a[p] += sa[x][p];
					b[p] += sb[x][p];
				}
			}
			int score = 0;
			for (int p = 0; p < WH; ++p) {
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
				if (start[p][0] == 1 || start[p][start[p][0] - 1] != s) start[p][start[p][0]++] = s;
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
			else++b[p];
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
			else++b[p];
			return res;
		}
	}

	private String[] toAnswer(Cell m[]) {
		ArrayList<String> res = new ArrayList<>();
		int buf[][] = new int[Cell.values().length][Cell.values().length], c = 0;
		for (int i = 0; i < WH; ++i) {
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
					if (a != Cell.N && b != Cell.N && a != Cell.E && b != Cell.U && b != Cell.E && a != b)
						s.append(a.name() + " -> " + b.name() + " : " + buf[a.ordinal()][b.ordinal()] + "\n");
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

	private int[][] toArray(List<int[]> list) {
		int res[][] = new int[list.size()][];
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
