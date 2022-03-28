import re
import requests
from bs4 import BeautifulSoup
import pandas as pd
from pykrx import stock
from pykrx import bond
from tqdm import tqdm
import matplotlib.pyplot as plt
import usefb
import itertools
from collections import Counter
import json
from datetime import datetime
import pickle
from itertools import islice
import re

class StockCollection():

    # stockprice 가져오기
    def get_stock_price(self,start_day, end_day):
        print("start get_stock_price")
        def str_day(d):
            return d.strftime('%Y%m%d')

        # 개장일 얻기
        tmp = stock.get_market_ohlcv(start_day, end_day, "005930")
        days = list(map(str_day, tmp.index.to_list()))
        tmp = stock.get_market_ohlcv(days[0], market="ALL")
        #stock
        df_stock = tmp["종가"].to_frame(name=days[0]).T
        #vi
        tmp["종고"] = tmp["종가"].map(str) + "," + tmp["고가"].map(str)
        viPrice = tmp["종고"].to_frame(name=days[0]).T

        for day in tqdm(days[1:]):
            ddf = stock.get_market_ohlcv(day, market="ALL")
            #stock
            tmp_stock = ddf["종가"].to_frame(name=str(day)).T
            df_stock = pd.concat([df_stock, tmp_stock])
            df_stock = df_stock.astype('float')

            #vi
            ddf["종고"] = ddf["종가"].map(str) + "," + ddf["고가"].map(str)
            tmp_vi = ddf["종고"].to_frame(name=str(day)).T
            viPrice = pd.concat([viPrice, tmp_vi])

        def ttmp(df_stock):
            df_stock = df_stock.fillna(method='ffill')
            df_stock = df_stock.fillna(method='bfill')
            # df_stock = df_stock.dropna(axis=0)
            df = stock.get_market_ohlcv(days[-1], market="KOSPI")
            df2 = stock.get_market_ohlcv(days[-1], market="KOSDAQ")
            codeList = df.loc[df["저가"] != 0].index.tolist() + df2.loc[df2["저가"] != 0].index.tolist()
            df_stock = df_stock[codeList]
            def codeChangeName(code):
                name = stock.get_market_ticker_name(code)
                return name

            df_code = df_stock.columns.tolist()
            df_stock.columns = list(map(codeChangeName, df_code))
            return df_stock
        st = ttmp(df_stock)
        vi = ttmp(viPrice)
        self.load_csv(st,"stockPrice.csv")
        self.load_csv(vi,"viPrice.csv")

    def update_stock_price(self, endDay):
        def str_day(d):
            return d.strftime('%Y%m%d')
        today = datetime.today().date().strftime('%Y%m%d')
        addStock = self.read_csv("stockPrice.csv")
        addVi = self.read_csv("viPrice.csv")
        self.get_stock_price(endDay, today)
        stockP = self.read_csv("stockPrice.csv")[1:]
        stockVi = self.read_csv("viPrice.csv")[1:]
        if endDay != today:
            result1 = pd.concat([addStock, stockP])
            result1 = result1.fillna(method='bfill')
            result2 = pd.concat([addVi, stockVi])
            result2 = result2.fillna(method='bfill')
        else:
            return 0
        self.load_csv(result1,"stockPrice.csv")
        self.load_csv(result2,"viPrice.csv")


    def load_csv(self,stock,filename):
        stock.to_csv(filename, mode='w')

    def read_csv(self,filename):
            df_stock = pd.read_csv(filename, dtype=str, index_col=0)
            return df_stock

#HighestCorrelation 구하기
    def corr_stock(self,st):
        corr = st.astype('float') \
            .round(5)\
            .corr(method='pearson')\
            .fillna(method='ffill')\
            .fillna(method='bfill') \
            .fillna(0)
        # corr.to_csv("corr.csv", mode='w')
        return corr

    def max_corr(self,num,corr):
        mc = corr[:].replace("1.0", "0")
        mc = mc.astype('float')
        mc = mc.fillna(0)
        max_list = dict(mc.max())
        max_list = list(zip(mc.idxmax().values, max_list.keys(), max_list.values()))

        def sort2(mlist):
            a = sorted(mlist[0:2])
            a.append(mlist[2])
            a = tuple(a)
            return a
        tmp = list(set(map(sort2, max_list)))
        tmp = list(map(list, tmp))
        tmp.sort(key=lambda x: str(x[2]), reverse=True)
        return tmp[:num]

    def vi_count2(self, stockP):
        print("start vi_count")
        viDic = {}
        stockP = stockP.astype('string') \
            .fillna(method='ffill') \
            .fillna(method='bfill')
        # .fillna("1,1")
        for dayId in (range(1, stockP.shape[0])):
            yDay = stockP.iloc[dayId - 1]
            tDay = stockP.iloc[dayId]
            viList = []
            for codeId in range(stockP.shape[1]):
                yDayCPrice = yDay.iloc[codeId].split(",")[0]
                tDayHPrice = tDay.iloc[codeId].split(",")[1]
                if int(tDayHPrice) / int(yDayCPrice) > 1.1 and yDay.iloc[codeId] != tDay.iloc[codeId]:
                    # print(yDayCPrice, tDayHPrice,stockP.index[dayId],stockP.columns[codeId])
                    viList.append(stockP.columns[codeId])
                    viDic[stockP.index[dayId]] = viList
        permut = []
        for key in tqdm(viDic.keys()):
            p = list(itertools.permutations(viDic[key], 2))
            permut.extend(p)
        per = Counter(permut)
        return per

    def get_highestCorrelation(self):
        pd.set_option('display.max_seq_items', None)
        print("start get_highestCorrelation")
        num =105
        stock1 = pd.read_csv("stockPrice.csv", dtype=str, index_col=0)
        viStock = pd.read_csv("viPrice.csv", dtype=str, index_col=0)
        countList = [30, 90, 180, 365,540]
        tmp = []
        hDic = {}
        for c in tqdm(countList):
            stock2 = stock1[-c-1:]
            cd = stock2.columns.tolist()
            d = str(stock2.iloc[0].name)
            df = stock.get_market_ohlcv(d, market="KOSPI")
            df2 = stock.get_market_ohlcv(d, market="KOSDAQ")
            codeList = df.loc[df["저가"] != 0].index.tolist() + df2.loc[df2["저가"] != 0].index.tolist()

            def codeChangeName(code):
                name = stock.get_market_ticker_name(code)
                return name

            codeList = list(map(codeChangeName, codeList))
            dd = set(cd) & set(codeList)
            st= stock2[list(dd)]
            corr = self.corr_stock(st)
            count =self.vi_count2(viStock[-c-1:]).most_common()
            def sort2(mlist):
                t=[]
                a = sorted(list(mlist[0]))
                t.extend(a)
                t.append(str(mlist[1]))
                t = tuple(t)
                return t
            tmp = list(set(map(sort2, list(count))))
            tmp = list(map(list, tmp))
            tmp.sort(key=  lambda x:int(x[2]), reverse=True)
            tmp = tmp[0:50]
            corr = corr.astype('string')
            max_corr_list = self.max_corr(50, corr)
            for m in max_corr_list:
                m[2] = str(round(m[2], 5))
            dic = {str(i + 1): ",".join(max_corr_list[i]+tmp[i]) for i in range(len(max_corr_list))}
            hDic[c]= dic
            # fb.load_best_corr(c, dic)
        return hDic
     # corrAll 구하기
    def search_max_corr(self,code, corr):
        max_corr_index = corr.loc[code].sort_values(ascending=False).index.tolist()
        max_corr = corr.loc[code].sort_values(ascending=False).tolist()
        return [[max_corr_index[i], max_corr[i]] for i in range(len(max_corr))]
    def get_corrAll(self):
        print("start get_corrAll")
        stock = pd.read_csv("stockPrice.csv", dtype=str, index_col=0)
        dic = dict.fromkeys(stock.columns, dict.fromkeys([str(i) for i in range(1, len(stock.columns) + 1)], ""))
        countList = [30, 90, 180, 365,540]
        for c in tqdm(countList):
            corr = self.corr_stock(stock[-c-1:])
            # 상관계수가 본인과 본인 외에 1인 경우는 오류라 0으로 변환
            for j in range(len(corr.index)):
                corr.iloc[j, j] = 9999999999.0
            corr = corr[:].replace(1, 0)
            for j in range(len(corr.index)):
                corr.iloc[j, j] = 1
            # 한 종목의 관계계수 높은 순서대로 검색
            for code in corr.index:
                tmp = dic[code].copy()
                search = self.search_max_corr(code, corr)
                for i in range(0, len(search)):
                    search[i][1] = str(round(search[i][1], 5))
                    if c == 540:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1]
                    else:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1] + ";"
                dic[code] = tmp
        return dic
    def vi_count(self,stockP):
        print("start vi_count")
        viDic = {}
        stockP = stockP.astype('string')\
            .fillna(method='ffill')\
            .fillna(method='bfill')
            # .fillna("1,1")
        for dayId in (range(1, stockP.shape[0])):
            yDay = stockP.iloc[dayId - 1]
            tDay = stockP.iloc[dayId]
            viList = []
            for codeId in range(stockP.shape[1]):
                yDayCPrice = yDay.iloc[codeId].split(",")[0]
                tDayHPrice = tDay.iloc[codeId].split(",")[1]
                if int(tDayHPrice) / int(yDayCPrice) > 1.1 and yDay.iloc[codeId] != tDay.iloc[codeId]:
                    # print(yDayCPrice, tDayHPrice,stockP.index[dayId],stockP.columns[codeId])
                    viList.append(stockP.columns[codeId])
                    viDic[stockP.index[dayId]] = viList
        permut = []
        for key in tqdm(viDic.keys()):
            p= list(itertools.product(viDic[key], repeat = 2))
            # p = list(itertools.permutations(viDic[key], 2))
            permut.extend(p)
        per= Counter(permut)
        return per

    def vi_count_df(self, p,col):
        df = pd.DataFrame(index=col, columns=col)
        df = df.fillna(0)
        for key,value in (p.items()):
            df.loc[key[0], key[1]] += int(value)
        return  df

    def get_vi_count(self):
        print("start get_vi_count")
        vi = pd.read_csv("viPrice.csv", dtype=str, index_col=0)
        dic = dict.fromkeys(vi.columns, dict.fromkeys([str(i) for i in range(1, len(vi.columns) + 1)], ""))
        countList = [30, 90, 180, 365,540]
        for c in tqdm(countList):
            viCount = self.vi_count(vi[-c:])
            viCount = self.vi_count_df(viCount,vi[-c:].columns)
            for code in viCount.index:
                tmp = dic[code].copy()
                search = self.search_max_corr(code, viCount)
                for i in range(0, len(search)):
                    search[i][1] = str(search[i][1])
                    if c == 540:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1]
                    else:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1] + ";"
                dic[code] = tmp
        return dic

    def get_viAndCorr(self):
        CA=self.get_corrAll()
        VC=self.get_vi_count()
        for i in CA.keys():
            for j in CA[i].keys():
                CA[i][j] = CA[i][j] + ";" + VC[i][j]
        with open('CA.pickle', 'wb') as fw:
            pickle.dump(CA, fw)
        return CA
    def sectors_corrAll(self,dic1,dic2):
        stock = pd.read_csv("stockPrice.csv", dtype=str, index_col=0)
        dic = dict.fromkeys(stock.columns, dict.fromkeys([str(i) for i in range(1, 200)], ""))
        countList = [30, 90, 180, 365,540]
        for c in (countList):
            stt= stock[-c-1:]
            corr = self.corr_stock(stt)
            # 상관계수가 본인과 본인 외에 1인 경우는 오류라 0으로 변환
            for j in range(len(corr.index)):
                corr.iloc[j, j] = 9999999999.0
            corr = corr[:].replace(1, 0)
            for j in range(len(corr.index)):
                corr.iloc[j, j] = 1
            # # 한 종목의 관계계수 높은 순서대로 검색
            ccd = set(stt.columns) & set(dic2.keys())
            for code in tqdm(ccd):
                sect = dic2[code]
                cd = list(set(dic1[sect]) & set(ccd))
                tmp = dic[code].copy()
                crr = corr.loc[cd,cd]
                search = self.search_max_corr(code, crr)
                for i in range(0, len(search)):
                    search[i][1] = str(round(search[i][1], 5))
                    if c == 540:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1]
                    else:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1] + ";"
                dic[code] = tmp
        for k in dic.keys():
            for k2 in dic[k].keys():
                # print(k2)
                # numbers = str(re.sub(r'[^0-9]', '', k2))
                # print(numbers)
                if dic[k][k2]=="":
                    numbers = str(re.sub(r'[^0-9]', '', k2))
                    dic[k]=dict(islice(dic[k].items(), int(numbers)-1))
                    break
        return dic

    def sectors_vi_count(self,dic1,dic2):
        vi = pd.read_csv("viPrice.csv", dtype=str, index_col=0)
        dic = dict.fromkeys(vi.columns, dict.fromkeys([str(i) for i in range(1, 200)], ""))
        countList = [30, 90, 180, 365, 540]
        for c in tqdm(countList):
            viCount = self.vi_count(vi[-c:])
            viCount = self.vi_count_df(viCount, vi[-c:].columns)
            ccd = set(viCount.columns) & set(dic2.keys())
            for code in ccd:
                sect = dic2[code]
                cd = list(set(dic1[sect]) & set(ccd))
                vc = viCount.loc[cd, cd]
                tmp = dic[code].copy()
                search = self.search_max_corr(code, vc)
                for i in range(0, len(search)):
                    search[i][1] = str(search[i][1])
                    if c == 540:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1]
                    else:
                        tmp[str(i + 1)] = tmp[str(i + 1)] + search[i][0] + ";" + search[i][1] + ";"
                dic[code] = tmp
        for k in dic.keys():
            for k2 in dic[k].keys():
                # print(k2)
                # numbers = str(re.sub(r'[^0-9]', '', k2))
                # print(numbers)
                if dic[k][k2]=="":
                    numbers = str(re.sub(r'[^0-9]', '', k2))
                    dic[k]=dict(islice(dic[k].items(), int(numbers)-1))
                    break
        return dic

    def get_sectors_viAndCorr(self,dic1,dic2):
        print("start get_sectors_viAndCorr")
        CA=self.sectors_corrAll(dic1,dic2)
        VC=self.sectors_vi_count(dic1,dic2)
        for i in CA.keys():
            for j in CA[i].keys():
                CA[i][j] = CA[i][j] + ";" + VC[i][j]
        with open('CA.pickle', 'wb') as fw:
            pickle.dump(CA, fw)
        return CA

    def get_sectors(self):
        def get_stocks( market=None):
            market_type = ''
            if market == 'kospi':
                market_type = '&marketType=stockMkt'
            elif market == 'kosdaq':
                market_type = '&marketType=kosdaqMkt'
            elif market == 'konex':
                market_type = '&marketType=konexMkt'
            url = 'http://kind.krx.co.kr/corpgeneral/corpList.do?currentPageSize=5000&pageIndex=1&method=download&searchType=13{market_type}'.format(
                market_type=market_type)
            list_df_stocks = pd.read_html(url, header=0, converters={'종목코드': lambda x: str(x)})
            df_stocks = list_df_stocks[0]
            return df_stocks
        kp = get_stocks("kospi")
        kd = get_stocks("kosdaq")
        df_stock = pd.concat([kp, kd])[["회사명", "업종"]]
        d = df_stock.groupby(['업종']).value_counts()
        d2 = df_stock.groupby(['업종']).count()
        sectors = d2.index
        dic = {}
        for i in sectors:
            dic[i] = list(d[i].index)
        df_stock.set_index("회사명", inplace=True)
        dic2 = df_stock.to_dict()["업종"]
        return dic, dic2

    #그래프 그리는거
    # def max_corr_graph(self,num,stock,max_corr_list):
    #     test = max_corr_list[0:num]
    #     a = [i[0:2] for i in test]
    #     for i in a:
    #         self.draw_graph(stock, i)
    #     plt.show()
    #
    # def draw_graph(self,stock,code_list):
    #     #정규화
    #     def minmax_norm(df):
    #         return (df - df.min()) / (df.max() - df.min())
    #     minmax_norm(stock[code_list].astype('float')).plot()
    #     plt.show()

    # def stock_ohlc(self,start_day, end_day):
    #     def str_day(d):
    #         return d.strftime('%Y%m%d')
    #
    #     tmp = stock.get_market_ohlcv(start_day, end_day, "005930")
    #     days = list(map(str_day, tmp.index.to_list()))
    #
    #     tmp = stock.get_market_ohlcv(days[0], market="ALL")
    #     tmp["시고저고"] = tmp["시가"].map(str) + "," + tmp["종가"].map(str) + "," + tmp["저가"].map(str) + "," + tmp["고가"].map(
    #         str)
    #
    #     df_stock = tmp["시고저고"].to_frame(name=days[0]).T
    #     for day in tqdm(days[1:]):
    #         ddf = stock.get_market_ohlcv(day, market="ALL")
    #         ddf["시고저고"] = ddf["시가"].map(str) + "," + ddf["종가"].map(str) + "," + ddf["저가"].map(str) + "," + ddf[
    #             "고가"].map(
    #             str)
    #
    #         a = ddf["시고저고"].to_frame(name=str(day)).T
    #         df_stock = pd.concat([df_stock, a])
    #
    #     # df_stock = df_stock.astype('float')
    #     df_stock = df_stock.fillna(method='ffill')
    #     df_stock = df_stock.fillna(method='bfill')
    #     # df_stock = df_stock.dropna(axis=0)
    #     df_stock.to_csv("ohlc.csv", mode='w')
    #
    #     return df_stock

