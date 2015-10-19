/* Change log
2015-10-12 : Remove ret from search method
*/

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JPanel;

class Path {
	int startRow, startCol;

	String dirs;

	Path(int row, int col, String dirs) {
		startRow = row;
		startCol = col;
		this.dirs = dirs;
	}

	public String toString() {
		return "(" + startRow + "," + startCol + ") " + dirs;
	}
}

public class MazeFixingVis {
	static final int MAX_TIME = 10000;
	// east, south, west, north - absolute directions
	final int[] dr = { 0, 1, 0, -1 };
	final int[] dc = { 1, 0, -1, 0 };
	final char[] rls = { 'R', 'U', 'L', 'S', 'E' };
	String[] maze;
	char[][] M;
	int W, H, F, N;

	// -----------------------------------------
	void generate(long seed) {
		try {
			//generate the maze
			SecureRandom r1 = SecureRandom.getInstance("SHA1PRNG");
			r1.setSeed(seed);
			if (seed <= 3) {
				W = H = (int) seed * 10;
			} else if (seed == 10) {
				W = H = 80;
			} else {
				W = r1.nextInt(71) + 10;
				H = r1.nextInt(71) + 10;
			}
			int i, j;
			M = new char[H][W];
			for (i = 0; i < H; i++)
				M[i][0] = M[i][W - 1] = '.';
			for (j = 0; j < W; j++)
				M[0][j] = M[H - 1][j] = '.';

			for (i = 1; i < H - 1; i++)
				for (j = 1; j < W - 1; j++)
					M[i][j] = rls[r1.nextInt(29) / 7];
			// remove corners randomly and mirror the result 3 times
			int crem = W / 2 - 1, rrem = r1.nextInt(H / 4) + H / 4 - 1;
			N = (W - 2) * (H - 2);
			for (i = 1; i < rrem; i++) {
				if (crem < 3) break;
				crem = r1.nextInt(crem / 3) + (2 * crem) / 3;
				for (j = 1; j < crem; j++) {
					M[i][j] = '.';
					M[H - i - 1][j] = '.';
					M[i][W - j - 1] = '.';
					M[H - i - 1][W - j - 1] = '.';
					N -= 4;
				}
			}
			F = r1.nextInt(N / 3 - N / 10) + N / 10;
			addFatalError("W = " + W + ", H = " + H + ", cells in maze N = " + N + ", F = " + F);

			maze = new String[H];
			for (i = 0; i < H; i++)
				maze[i] = new String(M[i]);
			if (debug) for (i = 0; i < H; i++)
				addFatalError(maze[i]);

			// preprocess: mark all border cells with *
			for (int r = 0; r < H; ++r)
				for (int c = 0; c < W; ++c) {
					if (M[r][c] != '.') continue;
					for (i = 0; i < 4; ++i)
						if (!outside(r + dr[i], c + dc[i])) M[r][c] = '*';
				}
		} catch (Exception e) {
			addFatalError("An exception occurred while trying to get your program's results.");
			e.printStackTrace();
		}
	}

	// -----------------------------------------
	boolean outside(int r, int c) {
		if (r < 0 || r > H - 1 || c < 0 || c > W - 1) return true;
		if (M[r][c] == '.' || M[r][c] == '*') return true;
		return false;
	}

	// -----------------------------------------
	boolean[][] visitedOnePath;

	boolean[][] visitedOverall;

	Vector<Path> paths;

	void search(int curR, int curC, int curDir, int curLen, int startRow, int startCol, String pathSoFar) {
		// right now we're at cell [curR][curC], facing in curDir direction
		// do one step in that direction, update direction accordingly to the cell we got onto, update length, continue until exit
		// we mark cell visited upon entry, not for cell we're currently in
		int newR = curR + dr[curDir];
		int newC = curC + dc[curDir];
		if (visitedOnePath[newR][newC]) {
			// cell already visited, can't continue
			return;
		}
		visitedOnePath[newR][newC] = true;
		String newPath = pathSoFar + curDir;
		// proceed from new cell in new direction depending on the type of the cell
		switch (M[newR][newC]) {
		case '*':
			// end of path - mark it on overall visited tracker
			for (int r = 0; r < H; ++r)
				for (int c = 0; c < W; ++c)
					visitedOverall[r][c] = visitedOverall[r][c] || visitedOnePath[r][c];
			// store it for visualization
			paths.add(new Path(startRow, startCol, newPath));
			break;
		case 'S':
			search(newR, newC, curDir, curLen + 1, startRow, startCol, newPath);
			break;
		case 'R':
			search(newR, newC, (curDir + 1) % 4, curLen + 1, startRow, startCol, newPath);
			break;
		case 'U':
			search(newR, newC, (curDir + 2) % 4, curLen + 1, startRow, startCol, newPath);
			break;
		case 'L':
			search(newR, newC, (curDir + 3) % 4, curLen + 1, startRow, startCol, newPath);
			break;
		case 'E':
			for (int newDir = 0; newDir < 4; ++newDir)
				search(newR, newC, newDir, curLen + 1, startRow, startCol, newPath);
		}
		visitedOnePath[newR][newC] = false;
	}

	// -----------------------------------------
	double getScore(long seed) {
		// count the % of cells visited by good paths in the maze. path must
		// 1) start in any * (. cell adjacent to a non-. cell)
		// 2) end in any * cell
		// 3) contain no loops (i.e. can't visit one cell twice)

		visitedOnePath = new boolean[H][W];
		visitedOverall = new boolean[H][W];
		paths = new Vector<>();
		// start from each * cell and do full search to find all paths which go from it
		for (int r = 0; r < H; ++r)
			for (int c = 0; c < W; ++c)
				if (M[r][c] == '*') for (int dir = 0; dir < 4; ++dir)
					if (!outside(r + dr[dir], c + dc[dir])) search(r, c, dir, 0, r, c, "");
		// score is % of maze cells visited on valid paths
		int nvis = 0;
		for (int r = 0; r < H; ++r)
			for (int c = 0; c < W; ++c)
				if (!outside(r, c) && visitedOverall[r][c]) nvis++;
		System.out.println("seed = " + seed + "   Score = " + nvis + " / " + N + "    " + ((double) nvis / N));
		return (double) nvis / N;
	}

	// -----------------------------------------
	public double runTest(long seed) {
		try {
			generate(seed);

			// call the solution
			String[] ret = new MazeFixing2().improve(maze, F);

			// check the params of the return
			if (ret.length > F) {
				addFatalError("Your return contained more than F elements.");
				return 0;
			}
			for (int i = 0; i < ret.length; i++) {
				String[] s = ret[i].split(" ");
				int R, C;
				char T;
				if (s.length != 3) {
					addFatalError("Element " + i + " of your return must be formatted as \"R C T\"");
					return 0;
				}
				// check the cell we want to change
				try {
					R = Integer.parseInt(s[0]);
					C = Integer.parseInt(s[1]);
				} catch (Exception e) {
					addFatalError("R and C in element " + i + " of your return must be integers.");
					return 0;
				}
				if (outside(R, C)) {
					addFatalError("R and C in element " + i + " of your return must specify a cell within the maze.");
					return 0;
				}

				// check the type of cell we're changing it into
				if (s[2].length() != 1) {
					addFatalError("T in element " + i + " of your return must be a single character.");
					return 0;
				}
				T = s[2].charAt(0);
				if (T != 'R' && T != 'U' && T != 'L' && T != 'S') {
					addFatalError("T in element " + i + " of your return must be 'R','U','L' or 'S'.");
					return 0;
				}

				if (M[R][C] == 'E') {
					addFatalError("Element " + i + " of your return is trying to change a cell that contains 'E'.");
					return 0;
				}

				// finally, apply the change
				M[R][C] = T;
			}

			// score the result - the *added* score
			double fixedScore = getScore(seed);

			if (vis) {
				jf.setSize((W + 2) * SZ + 30, (H + 2) * SZ + 40);
				if (paths.size() > 0) pathIndex = 0;
				else pathIndex = -1;
				v.repaint();
				jf.setVisible(true);
			}

			if (debug) {
				addFatalError("Paths after fix: " + paths.size());
				System.out.println(paths);
			}
			return fixedScore;
		} catch (Exception e) {
			System.err.println("An exception occurred while trying to get your program's results.");
			e.printStackTrace();
			return 0.0;
		}
	}

	// ------------- server part -------------------
	public String checkData(String test) {
		return "";
	}

	// -----------------------------------------
	public String displayTestCase(long test) {
		generate(test);
		StringBuffer sb = new StringBuffer();
		sb.append("seed = " + test + "\n");
		sb.append("W = " + W + ", H = " + H + ", cells in maze N = " + N + "\n");
		sb.append("Cells to fix F = " + F + "\n");
		for (String m : maze)
			sb.append(m + "\n");
		return sb.toString();
	}

	// -----------------------------------------
	public double[] score(double[][] sc) {
		double[] res = new double[sc.length];
		// absolute - just a sum
		for (int i = 0; i < sc.length; i++) {
			res[i] = 0;
			for (int j = 0; j < sc[0].length; j++)
				res[i] += sc[i][j];
		}
		return res;
	}

	// ------------- visualization part ------------
	static boolean vis, debug;

	static int SZ;

	InputStream is;

	OutputStream os;

	BufferedReader br;

	JFrame jf;

	Vis v;

	volatile int pathIndex;

	// -----------------------------------------
	void draw() {
		if (!vis) return;
		v.repaint();
	}

	// -----------------------------------------
	public class Vis extends JPanel implements MouseListener, WindowListener {
		private static final long serialVersionUID = -3977460782037038240L;

		public void paint(Graphics g) {
			int i, j;
			//do painting here
			BufferedImage bi = new BufferedImage((W + 2) * SZ, (H + 2) * SZ, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D) bi.getGraphics();
			//background
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, (W + 2) * SZ, (H + 2) * SZ);
			//mark visited cells
			g2.setColor(new Color(0xDDDDDD));
			for (i = 0; i < H; i++)
				for (j = 0; j < W; j++)
					if (!outside(i, j) && visitedOverall[i][j]) g2.fillRect(SZ + j * SZ, SZ + i * SZ, SZ, SZ);

			//grid
			g2.setColor(Color.BLACK);
			for (i = 0; i <= H; i++)
				g2.drawLine(SZ + 0, SZ + i * SZ, SZ + W * SZ, SZ + i * SZ);
			for (i = 0; i <= W; i++)
				g2.drawLine(SZ + i * SZ, SZ + 0, SZ + i * SZ, SZ + H * SZ);
			//bold lines on the border of the maze (maze is symmetrical, so no worry about row order)
			for (i = 0; i < H; i++)
				for (j = 0; j <= W; j++)
					if (outside(i, j - 1) != outside(i, j)) g2.drawLine(SZ + j * SZ + 1, SZ + i * SZ, SZ + j * SZ + 1, SZ + (i + 1) * SZ);

			for (j = 0; j < W; j++)
				for (i = 0; i <= H; i++)
					if (outside(i - 1, j) != outside(i, j)) g2.drawLine(SZ + j * SZ, SZ + i * SZ + 1, SZ + (j + 1) * SZ, SZ + i * SZ + 1);
			//cells
			g2.setFont(new Font("Arial", Font.BOLD, 10));
			FontMetrics fm = g2.getFontMetrics();
			char[] ch = new char[1];
			for (i = 0; i < H; i++)
				for (j = 0; j < W; j++)
					if (!outside(H - i - 1, j)) {
						ch[0] = M[H - i - 1][j];
						g2.drawChars(ch, 0, 1, SZ * (j + 1) + (SZ / 2) - fm.charWidth(ch[0]) / 2,
								SZ * (H - i) + (SZ / 2) - 1 + fm.getHeight() / 2);
					}
			// path #pathIndex (can click through)
			if (pathIndex > -1 && paths.size() > 0) {
				g2.setColor(Color.BLUE);
				Path p = paths.elementAt(pathIndex);
				int r = p.startRow;
				int c = p.startCol;
				g2.fillOval(SZ + c * SZ + SZ / 4, SZ + r * SZ + SZ / 4, SZ / 2, SZ / 2);
				for (int d = 0; d < p.dirs.length(); ++d) {
					int newDir = (int) (p.dirs.charAt(d) - '0');
					int newR = r + dr[newDir];
					int newC = c + dc[newDir];
					g2.drawLine(SZ + c * SZ + SZ / 2, SZ + r * SZ + SZ / 2, SZ + newC * SZ + SZ / 2, SZ + newR * SZ + SZ / 2);
					r = newR;
					c = newC;
				}
			}

			g.drawImage(bi, 0, 0, (W + 2) * SZ, (H + 2) * SZ, null);
		}

		// MouseListener
		public void mouseClicked(MouseEvent e) {
			if (paths.size() > 0) {
				pathIndex = (pathIndex + 1) % paths.size();
				draw();
			}
		}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		//WindowListener
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}

		public void windowActivated(WindowEvent e) {}

		public void windowDeactivated(WindowEvent e) {}

		public void windowOpened(WindowEvent e) {}

		public void windowClosed(WindowEvent e) {}

		public void windowIconified(WindowEvent e) {}

		public void windowDeiconified(WindowEvent e) {}
	}

	// -----------------------------------------
	public MazeFixingVis() {
		try {
			//interface for runTest
			if (vis) {
				jf = new JFrame();
				v = new Vis();
				jf.getContentPane().add(v);
				if (vis) v.addMouseListener(v);
				jf.addWindowListener(v);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// -----------------------------------------
	public static void main(String[] args) {
		vis = false;
		SZ = 14;
		debug = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-vis")) vis = true;
			if (args[i].equals("-debug")) debug = true;
			if (args[i].equals("-size")) SZ = Integer.parseInt(args[++i]);
		}
		if (false) {
			vis = false;
			final long init = 56;
			final long last = 56;
			double sum = 0;
			for (long seed = init; seed <= last; ++seed) {
				sum += new MazeFixingVis().runTest(seed);
			}
			System.out.println(init + " ~ " + last + " : " + sum);
		} else {
			new MazeFixingVis().test();
		}
	}

	void test() {
		class ParameterClass {
			volatile double d;
			volatile int c;
			volatile int timeover;
		}
		vis = false;
		debug = false;
		final ParameterClass sum = new ParameterClass();
		ExecutorService es = Executors.newFixedThreadPool(2);

		for (int seed = 1, size = seed + 1000; seed < size; seed++) {
			final int Seed = seed;
			es.submit(() -> {
				try {
					long time = System.currentTimeMillis();
					double score = new MazeFixingVis().runTest(Seed);
					time = System.currentTimeMillis() - time;
					sum.d += score;
					++sum.c;
					if (time > MAX_TIME) sum.timeover++;
					System.out.println(String.format("%3d   %.3f   %4d   %.1f   %.3f   %d", Seed, score, time, sum.d, sum.d / sum.c,
							sum.timeover));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		es.shutdown();
	}

	// -----------------------------------------
	void addFatalError(String message) {
		System.out.println(message);
	}
}

class ErrorReader extends Thread {
	InputStream error;

	public ErrorReader(InputStream is) {
		error = is;
	}

	public void run() {
		try {
			byte[] ch = new byte[50000];
			int read;
			while ((read = error.read(ch)) > 0) {
				String s = new String(ch, 0, read);
				System.out.print(s);
				System.out.flush();
			}
		} catch (Exception e) {}
	}
}
