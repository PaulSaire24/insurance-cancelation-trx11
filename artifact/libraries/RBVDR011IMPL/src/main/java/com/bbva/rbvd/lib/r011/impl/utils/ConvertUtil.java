package com.bbva.rbvd.lib.r011.impl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertUtil.class);

    public static String escapeSpecialCharacters(String input) {
        LOGGER.info("***** ConvertUtil - escapeSpecialCharacters - START");

        // Reemplazar caracteres especiales con sus entidades HTML equivalentes
        input = input.replace("á", "&aacute;");
        input = input.replace("é", "&eacute;");
        input = input.replace("í", "&iacute;");
        input = input.replace("ó", "&oacute;");
        input = input.replace("ú", "&uacute;");
        input = input.replace("ñ", "&ntilde;");
        input = input.replace("Á", "&Aacute;");
        input = input.replace("É", "&Eacute;");
        input = input.replace("Í", "&Iacute;");
        input = input.replace("Ó", "&Oacute;");
        input = input.replace("Ú", "&Uacute;");
        input = input.replace("Ñ", "&Ntilde;");
        // Agregar reemplazos para más caracteres si es necesario

        LOGGER.info("***** ConvertUtil - escapeSpecialCharacters - END");

        return input;
    }

}
