package application;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import application.Utils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SolverController
{
	@FXML private ImageView puzzleImage;
	@FXML private ImageView pieceImage;
	@FXML private ImageView detectImage;
	@FXML private ImageView removeImage;

	@FXML private Slider slider;
	@FXML private Slider slider2;

	Point clickedPoint = new Point(0, 0);
	Mat oldFrame;

	Mat puzzle;
	Mat puzzleGray;

	Mat piece;
	Mat pieceGray;

	protected void init()
	{
//		this.threshold.setShowTickLabels(true);

		//load puzzle image
		String puzzlePath = "D:\\Code and load\\workspace_new\\PuzzleSolver\\images\\puzzle_cropped.jpg";
		this.puzzle = Imgcodecs.imread(puzzlePath, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
//		Imgproc.resize(this.puzzle, this.puzzle, new Size(this.puzzle.height() / 2, this.puzzle.width() / 2));
		this.puzzleGray = Imgcodecs.imread(puzzlePath, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
//		Imgproc.resize(this.puzzleGray, this.puzzleGray, new Size(this.puzzleGray.height() / 2, this.puzzleGray.width() / 2));

		this.puzzleImage.setPreserveRatio(true);
		this.updateImageView(this.puzzleImage, Utils.mat2Image(this.puzzle));

		//load piece image
		String piecePath = "D:\\Code and load\\workspace_new\\PuzzleSolver\\images\\resized\\p2_red.jpg";;
		this.piece = Imgcodecs.imread(piecePath , Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
//		Imgproc.resize(this.piece, this.piece, new Size(this.piece.height() / 2, this.piece.width() / 2));
		this.pieceGray = Imgcodecs.imread(piecePath , Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
//		Imgproc.resize(this.pieceGray, this.pieceGray, new Size(this.pieceGray.height() / 2, this.pieceGray.width() / 2));

		this.pieceImage.setPreserveRatio(true);
		this.updateImageView(this.pieceImage, Utils.mat2Image(this.piece));

		this.slider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
//            	Mat detected = doBackgroundRemoval(new_val.doubleValue(), slider2.getValue());
//            	Mat detected = doCanny(new_val.doubleValue());
//            	Mat detected = doSobel(new_val.doubleValue(), slider2.getValue());
            	Mat detected = doContours(new_val.doubleValue());

            	updateImageView(detectImage, Utils.mat2Image(detected));
            }
        });

		this.slider2.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
//            	Mat detected = doBackgroundRemoval(slider.getValue(), new_val.doubleValue());
//            	Mat detected = doCanny(new_val.doubleValue());
            	Mat detected = doSobel(slider.getValue(), new_val.doubleValue());

            	updateImageView(detectImage, Utils.mat2Image(detected));
            }
        });
	}

	private Mat doBackgroundRemovalAbsDiff(Mat currFrame)
	{
		Mat greyImage = new Mat();
		Mat foregroundImage = new Mat();

		if (oldFrame == null)
			oldFrame = currFrame;

		Core.absdiff(currFrame, oldFrame, foregroundImage);
		Imgproc.cvtColor(foregroundImage, greyImage, Imgproc.COLOR_BGR2GRAY);

		int thresh_type = Imgproc.THRESH_BINARY_INV;
//		if (this.inverse.isSelected())
//			thresh_type = Imgproc.THRESH_BINARY;

		Imgproc.threshold(greyImage, greyImage, 10, 255, thresh_type);
		currFrame.copyTo(foregroundImage, greyImage);

		oldFrame = currFrame;
		return foregroundImage;

	}

	private Mat doBackgroundRemovalFloodFill(Mat frame)
	{

		Scalar newVal = new Scalar(255, 255, 255);
		Scalar loDiff = new Scalar(50, 50, 50);
		Scalar upDiff = new Scalar(50, 50, 50);
		Point seedPoint = clickedPoint;
		Mat mask = new Mat();
		Rect rect = new Rect();

		// Imgproc.floodFill(frame, mask, seedPoint, newVal);
		Imgproc.floodFill(frame, mask, seedPoint, newVal, rect, loDiff, upDiff, Imgproc.FLOODFILL_FIXED_RANGE);

		return frame;
	}

	/**
	 * Perform the operations needed for removing a uniform background
	 *
	 * @param frame
	 *            the current frame
	 * @return an image with only foreground objects
	 */
	private Mat doBackgroundRemoval(Double threshold, Double threshold2)
	{
		// init
		Mat hsvImg = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();
		Mat thresholdImg = new Mat();

		int thresh_type = Imgproc.THRESH_BINARY_INV;
//		if (this.inverse.isSelected())
//			thresh_type = Imgproc.THRESH_BINARY;

		// threshold the image with the average hue value
		hsvImg.create(this.piece.size(), CvType.CV_8U);
		Imgproc.cvtColor(this.piece, hsvImg, Imgproc.COLOR_BGR2HSV);
		Core.split(hsvImg, hsvPlanes);

		// get the average hue value of the image
		double threshValue = this.getHistAverage(hsvImg, hsvPlanes.get(0));

		Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

		Imgproc.blur(thresholdImg, thresholdImg, new Size(threshold*10, threshold*10));

		// dilate to fill gaps, erode to smooth edges
		Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), (int) (threshold2 / 10));
		Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), (int) (threshold2 * 3 / 10));

		Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

		// create the new image
		Mat foreground = new Mat(this.piece.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
		this.piece.copyTo(foreground, thresholdImg);

		return foreground;
	}

	/**
	 * Get the average hue value of the image starting from its Hue channel
	 * histogram
	 *
	 * @param hsvImg
	 *            the current frame in HSV
	 * @param hueValues
	 *            the Hue component of the current frame
	 * @return the average Hue value
	 */
	private double getHistAverage(Mat hsvImg, Mat hueValues)
	{
		// init
		double average = 0.0;
		Mat hist_hue = new Mat();
		// 0-180: range of Hue values
		MatOfInt histSize = new MatOfInt(180);
		List<Mat> hue = new ArrayList<>();
		hue.add(hueValues);

		// compute the histogram
		Imgproc.calcHist(hue, new MatOfInt(0), new Mat(), hist_hue, histSize, new MatOfFloat(0, 179));

		// get the average Hue value of the image
		// (sum(bin(h)*h))/(image-height*image-width)
		// -----------------
		// equivalent to get the hue of each pixel in the image, add them, and
		// divide for the image size (height and width)
		for (int h = 0; h < 180; h++)
		{
			// for each bin, get its value and multiply it for the corresponding
			// hue
			average += (hist_hue.get(h, 0)[0] * h);
		}

		// return the average hue of the image
		return average = average / hsvImg.size().height / hsvImg.size().width;
	}

	/**
	 * Apply Canny
	 *
	 * @param frame
	 *            the current frame
	 * @return an image elaborated with Canny
	 */
	private Mat doCanny(Double threshold)
	{
		// init
		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();

		// convert to grayscale
		Imgproc.cvtColor(this.piece, grayImage, Imgproc.COLOR_BGR2GRAY);

		// reduce noise with a 3x3 kernel
		Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));

		// canny detector, with ratio of lower:upper threshold of 3:1
		Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * 3);

		// using Canny's output as a mask, display the result
		Mat dest = new Mat();
		this.piece.copyTo(dest, detectedEdges);

		return dest;
	}

	/**
	 * Apply Sobel
	 *
	 * @param frame
	 *            the current frame
	 * @return an image elaborated with Sobel derivation
	 */
	private Mat doSobel(Double thresholdX, Double thresholdY)
	{
		// init
//		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();
		int scale = 1;
		int delta = 0;
		int ddepth = CvType.CV_16S;
		Mat grad_x = new Mat();
		Mat grad_y = new Mat();
		Mat abs_grad_x = new Mat();
		Mat abs_grad_y = new Mat();

		// reduce noise with a 3x3 kernel
		Imgproc.GaussianBlur(this.piece, this.pieceGray, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT);

		// convert to grayscale
		Imgproc.cvtColor(this.pieceGray, this.pieceGray, Imgproc.COLOR_BGR2GRAY);

		// Gradient X
		// Imgproc.Sobel(grayImage, grad_x, ddepth, 1, 0, 3, scale,
		// this.threshold.getValue(), Core.BORDER_DEFAULT );
		Imgproc.Sobel(this.pieceGray, grad_x, ddepth, 1, 0);
		Core.convertScaleAbs(grad_x, abs_grad_x);

		// Gradient Y
		// Imgproc.Sobel(grayImage, grad_y, ddepth, 0, 1, 3, scale,
		// this.threshold.getValue(), Core.BORDER_DEFAULT );
		Imgproc.Sobel(this.pieceGray, grad_y, ddepth, 0, 1);
		Core.convertScaleAbs(grad_y, abs_grad_y);

		// Total Gradient (approximate)
		Core.addWeighted(abs_grad_x, (int) (thresholdX / 10), abs_grad_y, (int) (thresholdY / 10), 0, detectedEdges);
		// Core.addWeighted(grad_x, 0.5, grad_y, 0.5, 0, detectedEdges);

		return detectedEdges;
	}

	/**
	 * Apply Sobel
	 *
	 * @param frame
	 *            the current frame
	 * @return an image elaborated with Sobel derivation
	 */
	private Mat doContours(Double thresholdX)
	{
		Mat detectedEdges = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();

		Imgproc.cvtColor(this.piece, detectedEdges, Imgproc.COLOR_BGR2HSV);

		Core.split(detectedEdges, hsvPlanes);





		return detectedEdges;
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 *
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}

}
