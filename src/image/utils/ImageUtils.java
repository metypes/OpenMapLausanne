package image.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
//import java.io.IOException;


//import javax.imageio.ImageIO;
//import javax.imageio.ImageReader;
//import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods used to interact with images.
 */
public class ImageUtils {
    static int DELTA;
    final private static int MASK=0x00_00_FF;
    private static int WIDTH;

    private final static Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    private static final boolean equals(final int[] data1, final int[] data2) {
        final int length = data1.length;
        if (length != data2.length) {
            logger.info("File lengths are different.");
            return false;
        }
        boolean slightdifference=false;
       int counter=0, counter2=0, counter3=0;
        for(int i = 0; i < length; i++) {
            if(data1[i] != data2[i]) {

                //If the alpha is 0 for both that means that the pixels are 100%
                //transparent and the color does not matter. Return false if 
                //only 1 is 100% transparent.
                if((((data1[i] >> 24) & 0xff) == 0) && (((data2[i] >> 24) & 0xff) == 0)) {
                    logger.info("Both pixles at spot {} are different but 100% transparent.", Integer.valueOf(i));
                }else if(
                        (Math.abs(Integer.valueOf(data1[i]>>16 & MASK)-Integer.valueOf(data2[i]>>16 & MASK))<=DELTA) &
                        (Math.abs(Integer.valueOf(data1[i]>>8 & MASK)-Integer.valueOf(data2[i]>>8 & MASK))<=DELTA) &
                        (Math.abs(Integer.valueOf(data1[i] & MASK)-Integer.valueOf(data2[i] & MASK))<=DELTA) 
                        )
                        {
                	++counter2;
                    logger.debug("Both pixles at spot {} are different but differ of "+ 
                        (Math.abs(Integer.valueOf(data1[i]>>16 & MASK)-Integer.valueOf(data2[i]>>16 & MASK))) + 
                        " units for the red component, " + 
                        (Math.abs(Integer.valueOf(data1[i]>>8 & MASK)-Integer.valueOf(data2[i]>>8 & MASK))) + 
                        " units for the green component, " +
                        (Math.abs(Integer.valueOf(data1[i] & MASK)-Integer.valueOf(data2[i] & MASK))) + 
                        " units for the blue component" 
                        ,"("+Integer.valueOf(i)%WIDTH +", "+ Math.floorDiv(i, WIDTH)+")");
                }else if(
                        (Math.abs(Integer.valueOf(data1[i]>>16 & MASK)-Integer.valueOf(data2[i]>>16 & MASK))>DELTA) ||
                        (Math.abs(Integer.valueOf(data1[i]>>8 & MASK)-Integer.valueOf(data2[i]>>8 & MASK))>DELTA) ||
                        (Math.abs(Integer.valueOf(data1[i] & MASK)-Integer.valueOf(data2[i] & MASK))>DELTA) 
                        )
                        {
                	slightdifference=true;
                	++counter;
                    logger.debug("Both pixles at spot {} are different but differ of "+ 
                        (Math.abs(Integer.valueOf(data1[i]>>16 & MASK)-Integer.valueOf(data2[i]>>16 & MASK))) + 
                        " units for the red component, " + 
                        (Math.abs(Integer.valueOf(data1[i]>>8 & MASK)-Integer.valueOf(data2[i]>>8 & MASK))) + 
                        " units for the green component, " +
                        (Math.abs(Integer.valueOf(data1[i] & MASK)-Integer.valueOf(data2[i] & MASK))) + 
                        " units for the blue component" 
                        , "("+Integer.valueOf(i)%WIDTH +", "+ Math.floorDiv(i, WIDTH)+")");
                        }else {
                        	++counter3;
                    logger.debug("The pixel {} is different.", Integer.valueOf(i));
                }
            }
        }
        if(slightdifference){
        	logger.info("Both groups differ of " + ((double)(counter)/(double)data1.length)*100 +"% ({} displaced pixels > 1 unit difference)", counter +"/"+ data1.length);
        	logger.info("Both groups differ of " + ((double)(counter2)/(double)data1.length)*100 +"% ({} displaced pixels of 1 unit difference)", counter2 +"/"+ data1.length);
        	//logger.info("Both groups completely differ of " + ((double)(counter3)/(double)data1.length)*100 +"% ({} 100% displaced pixels)", counter3 +"/"+ data1.length);
        	return false;
        }else{
        logger.info("Both groups of pixels are the same.");
        return true;
        }
    }

    private static final int[] getPixels(final BufferedImage img, final File file) {
    	WIDTH=img.getWidth();
        final int width = img.getWidth();
        final int height = img.getHeight();
        int[] pixelData = new int[width * height];

        final Image pixelImg; 
        if (img.getColorModel().getColorSpace() == ColorSpace.getInstance(ColorSpace.CS_sRGB)) {
            pixelImg = img;
        } else {
            pixelImg = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null).filter(img, null);
        }

        final PixelGrabber pg = new PixelGrabber(pixelImg, 0, 0, width, height, pixelData, 0, width);

        try {
            if(!pg.grabPixels()) {
                throw new RuntimeException();
            }
        } catch (final InterruptedException ie) {
            throw new RuntimeException(file.getPath(), ie);
        }

        return pixelData;
    }

    /**
     * Gets the {@link BufferedImage} from the passed in {@link File}.
     * 
     * @param file The <code>File</code> to use.
     * @return The resulting <code>BufferedImage</code>
     */
    final static BufferedImage getBufferedImage(final File file) {
        Image image;

        try (final FileInputStream inputStream = new FileInputStream(file)) {
            // ImageIO.read(file) is broken for some images so I went this 
            // route
            image = Toolkit.getDefaultToolkit().createImage(file.getCanonicalPath());

            //forces the image to be rendered
            new ImageIcon(image);
        } catch(final Exception e2) {
            throw new RuntimeException(file.getPath(), e2);
        }

        final BufferedImage converted = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = converted.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return converted;
    }

    /**
     * Compares file1 to file2 to see if they are the same based on a visual 
     * pixel by pixel comparison. This has issues with marking images different
     * when they are not. Works perfectly for all images.
     * 
     * @param file1 First file to compare
     * @param file2 Second image to compare
     * @return <code>true</code> if they are equal, otherwise 
     *         <code>false</code>.
     */
    private final static boolean visuallyCompareJava(final File file1, final File file2) {
        return equals(getPixels(getBufferedImage(file1), file1), getPixels(getBufferedImage(file2), file2));
    }

    /**
     * Compares file1 to file2 to see if they are the same based on a visual 
     * pixel by pixel comparison. This has issues with marking images different
     * when they are not. Works perfectly for all images.
     * 
     * @param file1 Image 1 to compare
     * @param file2 Image 2 to compare
     * @return <code>true</code> if both images are visually the same.
     */
    public final static boolean visuallyCompare(final File file1, final File file2, int delta) {
    	DELTA=delta;
        logger.info("Start comparing \"{}\" and \"{}\".", file1.getPath(), file2.getPath());

        if(file1 == file2) {
            return true;
        }

        boolean answer = visuallyCompareJava(file1, file2);

        if(!answer) {
            logger.info("The files \"{}\" and \"{}\" are not pixel by pixel the same image. Manual comparison required.", file1.getPath(), file2.getPath());
        }

        logger.info("Finish comparing \"{}\" and \"{}\".", file1.getPath(), file2.getPath());

        return answer;
    }

    /**
     * @param file The image to check
     * @return <code>true</code> if the image contains one or more pixels with
     *         some percentage of transparency (Alpha)
     */
    public final static boolean containsAlphaTransparency(final File file) {
        logger.debug("Start Alpha pixel check for {}.", file.getPath());

        boolean answer = false;
        for(final int pixel : getPixels(getBufferedImage(file), file)) {
            //If the alpha is 0 for both that means that the pixels are 100%
            //transparent and the color does not matter. Return false if 
            //only 1 is 100% transparent.
            if(((pixel >> 24) & 0xff) != 255) {
                logger.debug("The image contains Aplha Transparency.");
                return true;
            }
        }

        logger.debug("The image does not contain Aplha Transparency.");
        logger.debug("End Alpha pixel check for {}.", file.getPath());

        return answer;
    }
}