import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MazeFixing2 {

	private static final int MAX_TIME = 9500;
	private final long endTime = System.currentTimeMillis() + MAX_TIME;

	private int W, WH, F, S[][], init[];

	public String[] improve(String[] maze, int F) {
		int H = maze.length;
		W = maze[0].length() - 1;
		WH = W * H;
		this.F = F;
		init = new int[WH];
		for (int i = 0; i < H; ++i) {
			for (int j = 0; j + 1 < maze[i].length() && getPos(i, j) < WH; ++j) {
				int p = getPos(i, j);
				init[p] = Cell.get(maze[i].charAt(j));
			}
		}
		{
			List<int[]> slist = new ArrayList<>();
			int dir[] = new int[] { 1, -1, W, -W };
			for (int i = 0; i < WH; ++i) {
				if (init[i] != Cell.N) {
					for (int d : dir) {
						int n = i + d;
						if (init[n] == Cell.N) slist.add(new int[] { i, -d });
					}
				}
			}
			S = toArray(slist);
		}
		XorShift rnd = new XorShift();
		State now = new State(init);
		int score = 0;
		int best[] = Arrays.copyOf(init, WH);
		int pos[] = new int[WH << 1], pi = 0;
		int dpos[] = new int[WH], f = 0;
		for (int p = 0; p < WH; ++p) {
			if (now.m[p] == Cell.U && f < F) {
				now.m[p] = Cell.S;
				++f;
			}
		}
		now.calc();
		int next[] = new int[6], x[] = new int[6];
		for (int turn = 0;; ++turn) {
			f = pi = 0;
			for (int p = 10; p < WH; ++p) {
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
				x[0] = 1;
				int dpi = rnd.next(F);
				if (dpi < f) {
					int a = dpos[dpi];
					x[(x[0] << 1)] = a;
					x[(x[0] << 1) + 1] = init[a];
					++x[0];
				}
				x[(x[0] << 1)] = pos[rnd.next(pi)];
				x[(x[0] << 1) + 1] = rnd.next(3);
				++x[0];
				int tmp = now.value(x, f);
				if (value < tmp) {
					value = tmp;
					System.arraycopy(x, 0, next, 0, x.length);
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
		private int m[], a[] = new int[WH], b[] = new int[WH];
		private int sa[][] = new int[S.length][WH], sb[][] = new int[S.length][WH];
		private int start[][] = new int[WH][64];
		private boolean use[] = new boolean[WH];

		State(int m[]) {
			this.m = Arrays.copyOf(m, WH);
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
				for (int p = 10; p < WH; ++p) {
					a[p] += sa[i][p];
					b[p] += sb[i][p];
				}
			}
		}

		int value(int change[], int f) {
			boolean ud[] = new boolean[S.length];
			int tmp[] = Arrays.copyOf(m, WH);
			int tmpA[] = new int[WH];
			int tmpB[] = new int[WH];
			int buf[] = new int[50], bi = 0;
			for (int i = 1, isize = change[0]; i < isize; ++i) {
				int p = change[(i << 1)];
				int c = change[(i << 1) + 1];
				if (tmp[p] != c) {
					for (int j = 1, jsize = start[p][0]; j < jsize; ++j) {
						int x = start[p][j];
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
				for (int p = 10; p < WH; ++p) {
					tmpA[p] -= sa[x][p];
					tmpB[p] -= sb[x][p];
				}
				dfs(tmpA, tmp, S[x][0], S[x][1], tmpB);
			}
			int ac = 0, bc = 0;
			for (int p = 10; p < WH; ++p) {
				if (a[p] + tmpA[p] > 0) ++ac;
				else if (b[p] + tmpB[p] > 0) ++bc;
			}
			// value
			return ((bc + ac) << 5) * (F - f) + ac * f;
		}

		int update(int change[]) {
			boolean ud[] = new boolean[S.length];
			int buf[] = new int[50], bi = 0;
			for (int i = 1, isize = change[0]; i < isize; ++i) {
				int p = change[(i << 1)];
				int c = change[(i << 1) + 1];
				if (m[p] != c) {
					for (int j = 1, jsize = start[p][0]; j < jsize; ++j) {
						int x = start[p][j];
						if (!ud[x]) {
							ud[x] = true;
							buf[bi++] = x;
						}
					}
					m[p] = c;
				}
			}
			for (int i = 0; i < bi; ++i) {
				int x = buf[i];
				for (int p = 10; p < WH; ++p) {
					if (sa[x][p] + sb[x][p] > 0) {
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
				for (int p = 10; p < WH; ++p) {
					a[p] += sa[x][p];
					b[p] += sb[x][p];
				}
			}
			int score = 0;
			for (int p = 10; p < WH; ++p) {
				if (a[p] > 0) ++score;
			}
			return score;
		}

		private boolean dfs(int s, int a[], int m[], int p, int d, int b[]) {
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
				int x = dir(m[p], d);
				if (use[p + x]) res = dfs(s, a, m, p + x, x, b);
			}
			use[p] = true;
			if (res) ++a[p];
			else++b[p];
			return res;
		}

		private boolean dfs(int a[], int m[], int p, int d, int b[]) {
			if (m[p] == Cell.N) return true;
			boolean res = false;
			use[p] = false;
			if (m[p] == Cell.E) {
				if (use[p + 1]) res = dfs(a, m, p + 1, 1, b);
				if (use[p - 1]) res |= dfs(a, m, p - 1, -1, b);
				if (use[p + W]) res |= dfs(a, m, p + W, W, b);
				if (use[p - W]) res |= dfs(a, m, p - W, -W, b);
			} else {
				int x = dir(m[p], d);
				if (use[p + x]) res = dfs(a, m, p + x, x, b);
			}
			use[p] = true;
			if (res) ++a[p];
			else++b[p];
			return res;
		}

		private int dir(int c, int d) {
			if (c == Cell.R) {
				if (d == 1) return W;
				else if (d == -1) return -W;
				else if (d == W) return -1;
				else if (d == -W) return 1;
			} else if (c == Cell.L) {
				if (d == 1) return -W;
				else if (d == -1) return W;
				else if (d == W) return 1;
				else if (d == -W) return -1;
			} else if (c == Cell.U) {
				return -d;
			}
			return d;
		}
	}

	private String[] toAnswer(int m[]) {
		ArrayList<String> res = new ArrayList<>();
		for (int i = 0; i < WH; ++i) {
			if (m[i] != init[i]) {
				res.add(getRow(i) + " " + getCol(i) + " " + Cell.get(m[i]));
			}
		}
		return res.toArray(new String[0]);
	}

	private static class Cell {
		static final int R = 0, L = 1, S = 2, U = 3, E = 4, N = 5;

		static final int get(char c) {
			if (c == 'R') return R;
			else if (c == 'L') return L;
			else if (c == 'U') return U;
			else if (c == 'S') return S;
			else if (c == 'E') return E;
			return N;
		}

		static final char get(int c) {
			if (c == R) return 'R';
			else if (c == L) return 'L';
			else if (c == U) return 'U';
			else if (c == S) return 'S';
			else if (c == E) return 'E';
			return 'N';
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
		private int x = 123456789;
		private int y = 362436069;
		private int z = 521288629;
		private int w = 88675123;

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
