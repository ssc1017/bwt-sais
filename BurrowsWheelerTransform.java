import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class BurrowsWheelerTransform {

    private static final int L_TYPE = 0;

    private static final int S_TYPE = 1;

    private Map<Character, Integer> mapping = new HashMap<>();  // map each letter of the original string and the N number

    /**
     * convert String into T[] with N numbers
     * @param s
     * @param T
     * @return the size of the charset
     */
    private int preprocess(String s, int[] T) {
        int n = s.length();
        Map<Integer, Character> bucket = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Character c = s.charAt(i);
            if (!bucket.containsKey((int) c)) bucket.put((int) c, c);
        }
        int k = 1;
        for (int i = 0; i < 128; i++) {
            if (bucket.containsKey(i)) {
                mapping.put(bucket.get(i), k++);
            }
        }
        for (int i = 0; i < n; i++) {
            T[i] = mapping.get(s.charAt(i));
        }
        T[n] = 0;
        return k;
    }

    private void assignType(int[] T, int n, int[] type) {
        type[n - 1] = S_TYPE;  // define '$' as S type
        for (int i = n - 2; i >= 0; i--) {
            if (T[i] < T[i + 1]) {
                type[i] = S_TYPE;
            } else if (T[i] > T[i + 1]) {
                type[i] = L_TYPE;
            } else {
                type[i] = type[i + 1];
            }
        }
    }

    private void inducedSort(int[] T, int[] SA, int[] type, int[] cBucket, int[] lBucket, int[] sBucket, int n, int k) {
        // if c = T[SA[i] − 1] is L-type, then put SA[i] − 1 to the current head of the L-type c-bucket and forward the current head one item to the right.
        for (int i = 0; i < n; i++) {
            if (SA[i] > 0 && type[SA[i] - 1] == L_TYPE) {
                SA[lBucket[T[SA[i] - 1]]++] = SA[i] - 1;
            }
        }
        // reset S-type c-bucket
        for (int i = 1; i < k; i++) {
            sBucket[i] = cBucket[i] - 1;
        }
        // if c = T[SA[i] − 1] is S-type, put SA[i] − 1 to the current end of the S-type c-bucket and forward the current end one item to the left.
        for (int i = n - 1; i>=0; i--) {
            if (SA[i] > 0 && type[SA[i] - 1] == S_TYPE) {
                SA[sBucket[T[SA[i] - 1]]--] = SA[i] - 1;
            }
        }
    }

    private boolean isLmsChar(int[] type, int x) {
        return x > 0 && type[x] == S_TYPE && type[x - 1] == L_TYPE;
    }

    private boolean equalSubstring(int[] T, int x, int y, int[] type) {
        do {
            if (T[x] != T[y])
                return false;
            x++;
            y++;
        } while (!isLmsChar(type, x) && !isLmsChar(type, y));
        return T[x] == T[y];
    }

    /**
     * the implentation of SAIS, LMS is S*
     * @param T
     * @param n length of T
     * @param k the number of total alphabet in T
     * @return SA array
     */
    public int[] sais(int[] T, int n, int k) {
        int[] type = new int[n];  // type of each suffix
        int[] SA = new int[n];  // SA array
        int[] cBucket = new int[k];  // the start point of c-bucket
        int[] lBucket = new int[k];  // the start point of L-type c-bucket
        int[] sBucket = new int[k];  // the start point of S-type c-bucket
        int[] position = new int[n];
        int[] name = new int[n];

        // initialize all buckets
        for (int i = 0; i < n; i++) {
            cBucket[T[i]]++;
        }
        for (int i = 0; i < k - 1; i++) {
            cBucket[i + 1] += cBucket[i];
            lBucket[i + 1] = cBucket[i];  // from left to right
            sBucket[i + 1] = cBucket[i + 1] - 1;  // from right to left
        }

        assignType(T, n, type);
        
        // get the start index of each LMS substring
        int cnt = 0;
        for (int i = 1; i < n; i++) {
            if (type[i] == S_TYPE && type[i - 1] == L_TYPE || i == n - 1) {
                position[cnt++] = i;
            }
        }

        // sort LMS substrings
        for (int i = 0; i < n; i++) SA[i] = -1;
        for (int i = 0; i < cnt; i++) {
            SA[sBucket[T[position[i]]]--] = position[i];  // put the first letter of LMS substrings into the buckets
        }
        inducedSort(T, SA, type, cBucket, lBucket, sBucket, n, k);

        // rename LMS substrings
        for (int i = 0; i < n; i++) name[i] = -1;
        int lastX = -1;
        int nameCnt = 1;
        boolean flag = false;
        for (int i = 0; i < n; i++) {
            int x = SA[i];
            if (isLmsChar(type, x)) {
                if (lastX >= 0 && !equalSubstring(T, x, lastX, type)) {
                    nameCnt++;
                }
                if (lastX >= 0 && nameCnt == name[lastX])
                    flag = true;
                name[x] = nameCnt - 1;
                lastX = x;
            }
        }
        name[n - 1] = 0;
        
        // get S1
        int[] S1 = new int[cnt];
        int pos = 0;
        for (int i = 0; i < n; i++)
            if (name[i] >= 0)
                S1[pos++] = name[i];
        show(S1, "S1");

        int[] SA1;
        if (!flag) {
            SA1 = new int[cnt + 1];
            for (int i = 0; i < cnt; i++) {
                SA1[S1[i]] = i;
            }
        } else {
            SA1 = sais(S1, cnt, nameCnt);  // compute recursively
        }

        // induce SA from SA1
        lBucket[0] = 0;
        sBucket[0] = 0;
        // determine the start index for l and s buckets for each letter
        for (int i = 1; i < k; i++) {
            lBucket[i] = cBucket[i - 1];
            sBucket[i] = cBucket[i] - 1;
        }
        for (int i = 0; i < n; i++) SA[i] = -1;
        for (int i = cnt - 1; i >= 0; i--) {
            SA[sBucket[T[position[SA1[i]]]]--] = position[SA1[i]];
        }
        inducedSort(T, SA, type, cBucket, lBucket, sBucket, n, k);

        return SA;
    }

    private Character getKeyFromValue(Integer n) {
        Character key = null;
        for(Character c : mapping.keySet()) {
            if(mapping.get(c).equals(n)) {
                key = c;
                break;
            }
        }
        return key;
    }

    public String constructBwtFromSa(int[] T, int[] SA) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SA.length; i++) {
            if (SA[i] == 0) {
                // T[T.length - 1];
                sb.append('$');
            }
            else {
                sb.append(getKeyFromValue(T[SA[i] - 1]));
            }
        }
        return sb.toString();
    }

    private static void show(String s, String title) {
        System.out.println(title);
        System.out.println(s);
        System.out.println();
    }

    private static void show(int[] elements, String title) {
        System.out.println(title);
        int i = 0;
        for (; i < elements.length - 1; i++) {
            System.out.print(elements[i] + " ");
        }
        System.out.print(elements[i]);
        System.out.println();
        System.out.println();
    }
    
    public static void main(String[] args) {
        // SAIS method
        String s = new String("banana");
        show(s, "original String");
        BurrowsWheelerTransform burrowsWheelerTransform = new BurrowsWheelerTransform();
        int[] T = new int[s.length() + 1];
        int k = burrowsWheelerTransform.preprocess(s, T);
        show(T, "T");
        int[] SA = burrowsWheelerTransform.sais(T, T.length, k);
        show(SA, "SA");
        String bwt = burrowsWheelerTransform.constructBwtFromSa(T, SA);
        show(bwt, "BWT");
    }
}
