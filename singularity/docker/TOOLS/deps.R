
install.packages("data.table", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("plyr", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("dplyr", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("reshape", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("library", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("gap", dependencies=TRUE, repos="http://cran.rstudio.com/")
install.packages("sfsmisc", dependencies=TRUE, repos="http://cran.rstudio.com/")
if (!requireNamespace("BiocManager", quietly = TRUE, repos="http://cran.rstudio.com/"))
    install.packages("BiocManager", repos="http://cran.rstudio.com/")
#BiocManager::install("IRanges", version = "3.8")
BiocManager::install("IRanges")
