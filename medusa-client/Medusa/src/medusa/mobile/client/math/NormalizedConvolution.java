package medusa.mobile.client.math;

/**
 * <b>Normalized Convolution</b>
 * 
 * <p>
 * Description:
 * </p>
 * This class implements a fast version of the convolution of a filter,
 * represented as a vector, with another vector obtained from a given matrix.
 * For extracting the vector out of the matrix one can specify, if column or row
 * vectors should be used. This is done for all vectors in the matrix. <br>
 * <br>
 * For example if you like to blur a matrix using a gaussian filter, you hand in
 * the filter and specify, which dimension of the matrix to blur.<br>
 * <br>
 * Normalized means, that the filter vector used in the constructor will get
 * normalized, such that the sum of the filter weights is 1. Furthermore for the
 * first and the last elements, where one cannot apply the complete filter, a
 * truncated and normalized filter will be used.<br>
 * <br>
 * The general purpose is to perform the convolution in a fast way and to reuse
 * the convolution object for a large number of convolutions of the same type.
 * 
 * @author Klaus Seyerlehner
 * @version 1.0
 */
public class NormalizedConvolution {
	// fields
	protected double[] filter;

	// implementation details
	private double[][] normalizedFilterArray;

	/**
	 * Creates a <code>NormalizedConvolution</code> object, which can be used to
	 * compute the convolution a matrix with the given filter by using the
	 * <code>convolute()</code> method.
	 * 
	 * @param filter
	 *            double[] the filter used for the convolution e.g. a gaussian
	 *            filter
	 * 
	 * @throws IllegalArgumentException
	 *             the given filter must not be a null value and must have an
	 *             odd length greater or equal to 3
	 */
	public NormalizedConvolution(double[] filter)
			throws IllegalArgumentException {
		// check arguments
		if (filter == null)
			throw new IllegalArgumentException(
					"the given filter must not be a null value;");

		if (filter.length % 2 != 1 || filter.length < 3)
			throw new IllegalArgumentException(
					"the given filter must have an odd length greater or equal to 3;");

		// set fields
		this.filter = filter;

		// compute normalized filter array
		this.normalizedFilterArray = getNormalizedFilterArray(filter);
	}

	/**
	 * Perform the convolution of the given matrix according to the specified
	 * dimension. The filter has been fixed during the construction of the
	 * <code>NormalizedConvolution</code> object.
	 * 
	 * @param A
	 *            Matrix the matrix to convolute with
	 * @param firstDimension
	 *            boolean if true each of the row vectors will get convoluted
	 *            with the filter, otherwise each of the column vectors will be
	 *            used to convolute with
	 * @return Matrix as result the completely convoluted matrix will be
	 *         returned
	 */
	public Matrix convolute(Matrix A, boolean firstDimension) {
		double[][] data;
		Matrix B;

		// depending on the dimension either convolute the row or column vectors
		if (firstDimension) {
			data = A.getArrayCopy();

			// for each row vector compute the convolution with the filter
			for (int i = 0; i < data.length; i++)
				data[i] = computeConvolution(data[i], normalizedFilterArray);

			B = new Matrix(data);
		} else {
			B = A.transpose();
			data = B.getArray();

			// for each column vector compute the convolution with the filter
			for (int i = 0; i < data.length; i++)
				data[i] = computeConvolution(data[i], normalizedFilterArray);

			B = new Matrix(data);
			B = B.transpose();
		}
		return B;
	}

	/**
	 * Computes the convolution of a vector and a filter.
	 * 
	 * @param data
	 *            double[] the input vector to convolute with the filter
	 * @param filterArray
	 *            double[][] a filter array containing the normalized filter and
	 *            truncated normalized filters for the start and ending elements
	 *            of the vector.
	 * @return double[] the result of the convolution
	 */
	private double[] computeConvolution(double[] data, double[][] filterArray) {
		double[] output = new double[data.length];
		double sum = 0;
		int halfFilter = filterArray.length / 2;

		// compute for each position the convolution with the filter
		for (int i = 0; i < data.length; i++) {
			sum = 0;
			if (i < halfFilter) {
				// for the first elements use some truncated filters
				for (int j = 0; j < filterArray[i].length; j++)
					sum += filterArray[i][j] * data[j];
			} else if (i >= (data.length - halfFilter)) {
				// for the last elements use some truncated filters
				int filterArrayIndex = filterArray.length - (data.length - i);
				for (int j = 0; j < filterArray[filterArrayIndex].length; j++)
					sum += filterArray[filterArrayIndex][j]
							* data[i + j - halfFilter];
			} else {
				// for all other elements use the complete filter
				int filterArrayIndex = halfFilter;
				for (int j = 0; j < filterArray[filterArrayIndex].length; j++)
					sum += filterArray[filterArrayIndex][j]
							* data[i + j - halfFilter];
			}

			output[i] = sum;
		}

		return output;
	}

	/**
	 * Computes normalized and truncated filters and returns them in an array.
	 * 
	 * @param filter
	 *            double[] filter to compute normalized and truncated versions
	 *            of
	 * @return double[][] an array containing normalized and truncated version
	 *         of the given filter
	 */
	private double[][] getNormalizedFilterArray(double[] filter) {
		double[][] array = new double[filter.length][];

		int filterCount = 0;

		// computes all possible truncated versions of the filter
		for (int start = filter.length / 2; start > -filter.length / 2 - 1; start--) {
			array[filterCount] = new double[filter.length - Math.abs(start)];
			for (int i = 0, indexCount = 0; i + start < filter.length
					&& i < filter.length; i++) {
				if (start + i >= 0) {
					array[filterCount][indexCount] = filter[start + i];
					indexCount++;
				}
			}

			// normalize filter
			normalizeFilter(array[filterCount]);
			filterCount++;
		}

		return array;
	}

	/**
	 * Normalizes a given filter array in place.
	 * 
	 * @param filter
	 *            double[] the filter to normalize
	 */
	private void normalizeFilter(double[] filter) {
		double sum = 0;

		// compute filter sum
		for (int i = 0; i < filter.length; i++)
			sum += filter[i];

		// normalize
		for (int i = 0; i < filter.length; i++)
			filter[i] /= sum;
	}
}
