# blank-drr-base-model
a Blank repository template which could be used to create new model which are based on DRR Model


## Overview

Currently, report and projection tests have their pipeline IDs commented out. This is because no test packs specific to Monopoly have been generated.
To generate test packs please add inputs to the following path:

    - rosetta-source/main/resources

Providing inputs in rosetta-source/main/resources for trade/valuation/margin in enrich directory. An example file for trade has been provided.
Then run the test pack creator. These will generate the required test packs for the tests to run. After this you can uncomment the pipeline IDs in the tests.

