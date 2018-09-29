
package io.biocad.svgconv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SVGConvController {

    @RequestMapping(method = RequestMethod.POST, value = "/convert")
    public void convert(OutputStream out, InputStream in) throws IOException {

        SVGConverter converter = new SVGConverter();

        converter.convertSVG(in);

        converter.ppt.write(out);


    }

}
