import re
import matplotlib.pyplot as plt
from scipy.signal import find_peaks


def main(df):
    arr = re.findall("\d+\.\d+", df)
    t = arr[0::2]
    fpg = arr[1::2]
    peaks0, _ = find_peaks(fpg, distance = 12)
    t0 = []
    for i in peaks0:
      t0.append(t[i])
    return t0