import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class run2 {


    static class Point {
        final int r, c;
        Point(int r, int c) { this.r = r; this.c = c; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Point point = (Point) o; return r == point.r && c == point.c; }
        @Override public int hashCode() { return 31 * r + c; }
        @Override public String toString() { return "(" + r + "," + c + ")"; }
    }

    static class PathInfo {
        final int distance;
        final int requiredKeysMask;
        final int keysOnPathMask;

        static final PathInfo Unreachable = new PathInfo(Integer.MAX_VALUE, 0, 0);

        PathInfo(int distance, int requiredKeysMask, int keysOnPathMask) {
            this.distance = distance;
            this.requiredKeysMask = requiredKeysMask;
            this.keysOnPathMask = keysOnPathMask;
        }

        boolean isReachable() { return distance != Integer.MAX_VALUE; }
    }


    static class SearchState {
        final int keysMask;
        final int[] robotPoiIndices;

        SearchState(int keysMask, int[] robotPoiIndices) {
            this.keysMask = keysMask;
            this.robotPoiIndices = Arrays.copyOf(robotPoiIndices, robotPoiIndices.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchState that = (SearchState) o;
            return keysMask == that.keysMask && Arrays.equals(robotPoiIndices, that.robotPoiIndices);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(keysMask);
            result = 31 * result + Arrays.hashCode(robotPoiIndices);
            return result;
        }
        @Override
        public String toString() {
            return "State{keys=" + Integer.toBinaryString(keysMask) + ", pos=" + Arrays.toString(robotPoiIndices) + '}';
        }
    }

    static class PQEntry implements Comparable<PQEntry> {
        final int distance;
        final SearchState state;

        PQEntry(int distance, SearchState state) {
            this.distance = distance;
            this.state = state;
        }

        @Override
        public int compareTo(PQEntry other) {
            return Integer.compare(this.distance, other.distance);
        }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; PQEntry pqEntry = (PQEntry) o; return distance == pqEntry.distance && Objects.equals(state, pqEntry.state); } // Для потенциального использования в Set
        @Override public int hashCode() { return Objects.hash(distance, state); }
    }

    static class BfsState {
        final Point pos;
        final int distance;
        final int requiredKeysMask;
        final int keysOnPathMask;

        BfsState(Point pos, int distance, int requiredKeysMask, int keysOnPathMask) {
            this.pos = pos;
            this.distance = distance;
            this.requiredKeysMask = requiredKeysMask;
            this.keysOnPathMask = keysOnPathMask;
        }
    }


    private static int R, C;
    private static char[][] maze;
    private static List<Point> pointsOfInterest;
    private static Map<Point, Integer> poiMap;
    private static int[] poiKeyMask;
    private static int totalKeysMaskTarget;
    private static int numPoi;
    private static PathInfo[][] precomputedPaths;
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static void precomputePaths() {
        precomputedPaths = new PathInfo[numPoi][numPoi];
        for (int i = 0; i < numPoi; i++) {
            Arrays.fill(precomputedPaths[i], PathInfo.Unreachable);
            precomputedPaths[i][i] = new PathInfo(0, 0, 0); // Path to self

            bfsFromPoi(i);
        }
    }

    private static void bfsFromPoi(int startPoiIndex) {
        Point startPos = pointsOfInterest.get(startPoiIndex);
        Queue<BfsState> queue = new LinkedList<>();
        Map<Point, Integer> visitedDist = new HashMap<>();

        queue.offer(new BfsState(startPos, 0, 0, 0));
        visitedDist.put(startPos, 0);

        while (!queue.isEmpty()) {
            BfsState current = queue.poll();
            Point currentPos = current.pos;

            Integer targetPoiIndex = poiMap.get(currentPos);
            if (targetPoiIndex != null && targetPoiIndex != startPoiIndex) {
                if (current.distance < precomputedPaths[startPoiIndex][targetPoiIndex].distance) {
                    int keysOnPath = current.keysOnPathMask & ~poiKeyMask[targetPoiIndex];
                    precomputedPaths[startPoiIndex][targetPoiIndex] = new PathInfo(
                            current.distance,
                            current.requiredKeysMask,
                            keysOnPath
                    );

                }
            }

            for (int i = 0; i < 4; i++) {
                int nextR = currentPos.r + DR[i];
                int nextC = currentPos.c + DC[i];
                Point nextPos = new Point(nextR, nextC);

                if (nextR < 0 || nextR >= R || nextC < 0 || nextC >= C || maze[nextR][nextC] == '#') {
                    continue;
                }

                char nextCell = maze[nextR][nextC];
                int nextRequiredKeysMask = current.requiredKeysMask;
                int nextKeysOnPathMask = current.keysOnPathMask;
                int nextDistance = current.distance + 1;

                if (nextCell >= 'A' && nextCell <= 'Z') {
                    nextRequiredKeysMask |= (1 << (nextCell - 'A'));
                }
                else if (nextCell >= 'a' && nextCell <= 'z') {
                    nextKeysOnPathMask |= (1 << (nextCell - 'a'));
                }

                if (nextDistance < visitedDist.getOrDefault(nextPos, Integer.MAX_VALUE)) {
                    visitedDist.put(nextPos, nextDistance);
                    queue.offer(new BfsState(nextPos, nextDistance, nextRequiredKeysMask, nextKeysOnPathMask));
                }
            }
        }
    }


    private static int solveInternal() {
        pointsOfInterest = new ArrayList<>();
        poiMap = new HashMap<>();
        Map<Character, Point> keyLocations = new HashMap<>();
        List<Point> startPositions = new ArrayList<>();
        totalKeysMaskTarget = 0;

        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                char cell = maze[r][c];
                if (cell == '@') {
                    Point p = new Point(r, c);
                    startPositions.add(p);
                    if (!poiMap.containsKey(p)) {
                        poiMap.put(p, pointsOfInterest.size());
                        pointsOfInterest.add(p);
                    }
                    maze[r][c] = '.';
                } else if (cell >= 'a' && cell <= 'z') {
                    Point p = new Point(r, c);
                    keyLocations.put(cell, p);
                    totalKeysMaskTarget |= (1 << (cell - 'a'));
                    // Ключи - это PoI
                    if (!poiMap.containsKey(p)) {
                        poiMap.put(p, pointsOfInterest.size());
                        pointsOfInterest.add(p);
                    }
                }
            }
        }

        if (startPositions.size() != 4) {
            System.err.println("Недостаточное количество стартовых позиций " + startPositions.size());
            return Integer.MAX_VALUE;
        }
        if (totalKeysMaskTarget == 0) return 0;


        numPoi = pointsOfInterest.size();
        poiKeyMask = new int[numPoi];
        for (int i = 0; i < numPoi; i++) {
            Point p = pointsOfInterest.get(i);
            char cell = maze[p.r][p.c];
            for(Map.Entry<Character, Point> entry : keyLocations.entrySet()) {
                if (entry.getValue().equals(p)) {
                    cell = entry.getKey();
                    break;
                }
            }

            if (cell >= 'a' && cell <= 'z') {
                poiKeyMask[i] = (1 << (cell - 'a'));
            } else {
                poiKeyMask[i] = 0;
            }
        }

        precomputePaths();

        PriorityQueue<PQEntry> pq = new PriorityQueue<>();
        Map<SearchState, Integer> dist = new HashMap<>();

        int[] initialRobotPoiIndices = new int[4];
        for (int i = 0; i < 4; i++) {
            initialRobotPoiIndices[i] = poiMap.get(startPositions.get(i));
        }
        SearchState initialState = new SearchState(0, initialRobotPoiIndices); // Маска 0, роботы на стартовых PoI

        dist.put(initialState, 0);
        pq.offer(new PQEntry(0, initialState));

        while (!pq.isEmpty()) {
            PQEntry currentEntry = pq.poll();
            int currentTotalDist = currentEntry.distance;
            SearchState currentState = currentEntry.state;

            if (currentTotalDist > dist.getOrDefault(currentState, Integer.MAX_VALUE)) {
                continue;
            }

            if (currentState.keysMask == totalKeysMaskTarget) {
                return currentTotalDist;
            }

            for (int robotIndex = 0; robotIndex < 4; robotIndex++) {
                int currentPoiIndex = currentState.robotPoiIndices[robotIndex];

                for (int targetPoiIndex = 0; targetPoiIndex < numPoi; targetPoiIndex++) {
                    int targetKeyBit = poiKeyMask[targetPoiIndex];
                    if (targetKeyBit == 0 || (currentState.keysMask & targetKeyBit) != 0) {
                        continue;
                    }

                    PathInfo path = precomputedPaths[currentPoiIndex][targetPoiIndex];

                    if (!path.isReachable()) continue;

                    if ((currentState.keysMask & path.requiredKeysMask) != path.requiredKeysMask) {
                        continue;
                    }


                    if ((path.keysOnPathMask & ~currentState.keysMask) != 0) {
                        continue;
                    }


                    int nextKeysMask = currentState.keysMask | targetKeyBit;
                    int[] nextRobotPoiIndices = currentState.robotPoiIndices.clone();
                    nextRobotPoiIndices[robotIndex] = targetPoiIndex;

                    SearchState nextState = new SearchState(nextKeysMask, nextRobotPoiIndices);
                    int newTotalDist = currentTotalDist + path.distance;

                    if (newTotalDist < dist.getOrDefault(nextState, Integer.MAX_VALUE)) {
                        dist.put(nextState, newTotalDist);
                        pq.offer(new PQEntry(newTotalDist, nextState));
                    }
                }
            }
        }

        return Integer.MAX_VALUE;
    }



    private static char[][] getInput() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }
        R = lines.size();
        if (R == 0) return new char[0][0];
        C = lines.get(0).length();
        char[][] inputMaze = new char[R][C];
        for (int i = 0; i < R; i++) {
            inputMaze[i] = lines.get(i).toCharArray();
        }
        return inputMaze;
    }

    private static int solve(char[][] inputMaze) {
        if (inputMaze.length == 0 || inputMaze[0].length == 0) return 0;
        maze = inputMaze;
        return solveInternal();
    }


    public static void main(String[] args) throws IOException {
        char[][] data = getInput();
        int result = solve(data);

        if (result == Integer.MAX_VALUE) {
            System.out.println("No solution found");
        } else {
            System.out.println(result);
        }
    }
}