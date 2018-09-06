package ru.vvizard.rssspy.botservice.utils;

import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * Фильтр удаляет символы запрещенные для XML
 */
public class XmlReaderFilter extends XmlReader {

    public XmlReaderFilter(InputStream is) throws IOException {
        super(is);
    }

    @Override
    public int read() {
        int character = 0;
        try {
            character = super.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (character == 0x1)
            return 0x20;
        return character;
    }
}