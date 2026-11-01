# CDC BMI-for-age reference

Source: CDC-DNPAO's own [CDCAnthro package](https://github.com/CDC-DNPAO/CDCAnthro/blob/main/cdcanthro_0.1.3.tar.gz), `data/cdc__ref__data.rda`. Retrieved 2026-10-28. Public US government reference data; CDC is not endorsing this app.

Package SHA-256: `0c6f86ccb174363305f4ebf943c1726901faec0d43a4e67be0ad09a23fa09681`.

`cdc_bmi_for_age_lms.csv` contains only the original numeric `SEX`, `_AGEMOS1`, `_LBMI1`, `_MBMI1`, `_SBMI1` columns for half-month ages 24.5 through 239.5, both sexes: **432 rows**. CSV SHA-256: `8a6b2e62ae2d5cfb2750aed3e041a4b554cb465ac8970a71e2ee6370b8c10206`.

The generated `CdcBmiReferenceData.java` contains the same LMS rows as numeric constants, avoiding UI-thread asset I/O. Completed month N uses the published N+0.5 reference point for that month, as specified by [CDC](https://www.cdc.gov/growthcharts/cdc-data-files.htm). Years and additional months are entered explicitly; finer day-level interpolation is not claimed.

LMS transformation: `M × (1 + L × S × Z)^(1/L)` (or `M × exp(S × Z)` when L=0), with normal quantiles for the 5th, 85th and 95th percentiles. Classification compares unrounded mass with the reference BMI multiplied by height squared. Only percentile **bands** are displayed; an exact percentile above the 95th is not extrapolated from the original LMS curve. Severe obesity uses CDC's `BMI ≥120% of P95 OR BMI ≥35` rule.

The rows for the CDC-published example of a girl at 114.5 months match `L=-2.257782149, M=16.57626713, S=0.132796819`. Validate this and the published P95 examples independently when updating the data. Reference data are bundled offline; personal inputs are never sent to CDC.
