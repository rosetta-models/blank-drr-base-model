package com.regnosys.bank.testpack;

import com.google.common.collect.ImmutableList;

import java.nio.file.Path;

public class BankStatics {

    public static final ImmutableList<String> ROSETTA_PATHS = ImmutableList.of("bank/rosetta");
    public static final Path BANK_PRE_REPORT_INPUT_PATH = Path.of("enrich/input");
    public static final Path BANK_REPORT_INPUT_PATH = Path.of("regulatory-reporting/input");
    public static final Path BANK_FILE_DROP = Path.of("bank-file-drop");

    public static final Path WRITE_PATH = Path.of("rosetta-source/src/main/resources");
    public static final Path BANK_PROJECTION_PATH = WRITE_PATH.resolve("projection");
    public static final Path BANK_REGULATORY_REPORTING_PATH = WRITE_PATH.resolve("regulatory-reporting");
    public static final Path XML_SUBMISSIONS_PATH = Path.of("tests/src/test/resources/submissions");


}
