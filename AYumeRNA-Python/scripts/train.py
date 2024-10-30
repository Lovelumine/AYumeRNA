# scripts/train.py

# coding: UTF-8

import sys
sys.path.append("./src")
import os
import csv
import torch
import torch.nn as nn
import numpy as np

def modifiedCELoss(pred, soft_targets, gamma=0, summarize=True):
    """
    shape: (BATCH, N_COL, N_RULE)
    gamma for focal loss
    calc sum of a column and normalize a col by the sum, then calc CEloss.
    Sum CELoss for each column and normalize it by the number of columns.
    summarize: column-wise sum.
    """
    scale = soft_targets.nansum(dim=-1).unsqueeze(dim=-1)
    logsoftmax = nn.LogSoftmax(dim=-1)
    softmax = nn.Softmax(dim=-1)
    ce = -soft_targets / scale * ((1 - softmax(pred)).pow(gamma)) * logsoftmax(pred)
    ce_colwise = torch.sum(ce, dim=-1)
    if summarize:
        return torch.sum(ce)
    else:
        return ce.sum(dim=-1).sum(dim=-1)

def save_model(model, dir_name, pt_file):
    if not os.path.isdir(dir_name):
        os.mkdir(dir_name)
    torch.save(model.state_dict(), os.path.join(dir_name, pt_file))

def write_csv(d, dir_name, fname):
    if not os.path.isdir(dir_name):
        os.mkdir(dir_name)
    path = os.path.join(dir_name, fname)
    with open(path, 'w') as f:
        writer = csv.writer(f)
        writer.writerow(d.keys())
        writer.writerows(zip(*d.values()))
    print(f"Saved the log csv at {path}")

def train_main(args):
    import random
    import yaml
    from pprint import pprint
    from models.CMVAE import CovarianceModelVAE, MyDataset
    from util import Timer, AnnealKL
    from torch.utils.data import DataLoader

    # Set random seeds
    torch.manual_seed(args.get('random_seed', 42))
    np.random.seed(args.get('random_seed', 42))
    random.seed(args.get('random_seed', 42))

    timer = Timer()

    # Load training dataset
    train_dataset = MyDataset(
        path=os.path.join(args['data_dir'], args['X_train']),
        weight_path=os.path.join(args['data_dir'], args.get('w_train', '')) if args.get('w_train') else None
    )
    train_dataloader = DataLoader(train_dataset, batch_size=args.get('batch_size', 8), shuffle=True, num_workers=4, drop_last=True)

    # Load validation dataset if provided
    if not args.get('only_training', False) and args.get('X_valid'):
        valid_dataset = MyDataset(
            path=os.path.join(args['data_dir'], args['X_valid']),
            weight_path=os.path.join(args['data_dir'], args.get('w_valid', '')) if args.get('w_valid') else None
        )
        valid_dataloader = DataLoader(valid_dataset, batch_size=args.get('batch_size', 8), shuffle=True, num_workers=4, drop_last=True)
    else:
        valid_dataset = None
        valid_dataloader = None

    TR_LEN = train_dataset.data["tr"].shape[-2]
    S_LEN = train_dataset.data["s"].shape[-2]
    P_LEN = train_dataset.data["p"].shape[-2]
    DATA_SIZE = train_dataset.data["tr"].shape[0]

    conv_params = {
        "ker1": args.get('ker1', 5), "ch1": args.get('ch1', 5),
        "ker2": args.get('ker2', 5), "ch2": args.get('ch2', 5),
        "ker3": args.get('ker3', 7), "ch3": args.get('ch3', 8)
    }

    model = CovarianceModelVAE(
        hidden_encoder_size=args.get('hidden', 128),
        z_dim=args.get('z_dim', 16),
        hidden_decoder_size=args.get('hidden', 128),
        tr_len=TR_LEN,
        s_len=S_LEN,
        p_len=P_LEN,
        stride=args.get('stride', 1),
        # dropout_rate=args.get('dropout_rate', 0.0),
        conv_params=conv_params,
    )
    anneal = AnnealKL(step=(DATA_SIZE / args.get('batch_size', 8) * args.get('anneal_saturate_rate', 0.4)) ** -1, rate=args.get('anneal_rate', 1))
    beta_sum_batch = args.get('beta', 0.001) * args.get('batch_size', 8)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.get('learning_rate', 1e-3))

    def train_model(log):
        for step, tr_s_p_w in enumerate(train_dataloader, 0):
            tr, s, p, w = tr_s_p_w
            tr = tr.to(model.device)
            s = s.to(model.device)
            p = p.to(model.device)
            w = w.to(model.device)

            mu, logvar = model.encoder((tr, s, p))
            z = model.sample(mu, logvar)
            tr_, s_, p_ = model.decoder(z)
            tr_loss = modifiedCELoss(tr_.transpose(-1, -2), tr.transpose(-1, -2), summarize=False)
            s_loss = modifiedCELoss(s_.transpose(-1, -2), s.transpose(-1, -2), summarize=False)
            p_loss = modifiedCELoss(p_.transpose(-1, -2), p.transpose(-1, -2), summarize=False)
            loss = (tr_loss + s_loss + p_loss)
            w = w.reshape(args.get('batch_size', 8))
            loss = (w * (tr_loss + s_loss + p_loss)).sum()
            kl = model.kl(mu, logvar)

            alpha = beta_sum_batch * anneal.alpha(step) if args.get('use_anneal', False) else beta_sum_batch
            elbo = loss + alpha * kl

            # update parameters
            optimizer.zero_grad()
            elbo.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=args.get('clip', 20))
            optimizer.step()

            # Logging info
            log['loss'].append(loss.cpu().detach().numpy())
            log['kl'].append(kl.cpu().detach().numpy())
            log['elbo'].append(elbo.cpu().detach().numpy())
            log['alpha'].append(alpha)
            if step % args.get('print_every', 20) == 0:
                print(
                    '| step {}/{} \t| loss {:.4f}\t| kl {:.4f}\t|'
                    ' elbo {:.4f}\t| alpha {:.6f}\t| {:.0f} sents/sec\t|'.format(
                        step, DATA_SIZE // args.get('batch_size', 8),
                        np.mean(log['loss'][-args.get('print_every', 20):]),
                        np.mean(log['kl'][-args.get('print_every', 20):]),
                        np.mean(log['elbo'][-args.get('print_every', 20):]),
                        np.mean(log['alpha'][-1]),
                        args.get('batch_size', 8) * args.get('print_every', 20) / timer.elapsed()
                    )
                )
        return log

    def valid_model(log_valid):
        tmp_loss = []
        tmp_kl = []
        tmp_elbo = []

        for step, tr_s_p_w in enumerate(valid_dataloader, 0):
            tr, s, p, w = tr_s_p_w
            tr = tr.to(model.device)
            s = s.to(model.device)
            p = p.to(model.device)
            w = w.to(model.device)

            mu, logvar = model.encoder((tr, s, p))
            z = model.sample(mu, logvar)
            tr_, s_, p_ = model.decoder(z)
            tr_loss = modifiedCELoss(tr_.transpose(-1, -2), tr.transpose(-1, -2), summarize=False)
            s_loss = modifiedCELoss(s_.transpose(-1, -2), s.transpose(-1, -2), summarize=False)
            p_loss = modifiedCELoss(p_.transpose(-1, -2), p.transpose(-1, -2), summarize=False)
            w = w.reshape(args.get('batch_size', 8))
            loss = (w * (tr_loss + s_loss + p_loss)).sum()
            kl = model.kl(mu, logvar)
            elbo = loss + beta_sum_batch * kl

            tmp_loss.append(loss.item())
            tmp_kl.append(kl.item())
            tmp_elbo.append(elbo.item())

        log_valid["loss_valid"].append(np.mean(tmp_loss))
        log_valid["kl_valid"].append(np.mean(tmp_kl))
        log_valid["elbo_valid"].append(np.mean(tmp_elbo))

        print(
            '| valid {}/{}\t| loss {:.4f}\t| kl {:.4f}\t|'
            ' elbo {:.4f}\t|'.format(
                epoch, args.get('epoch', 200),
                log_valid["loss_valid"][-1],
                log_valid["kl_valid"][-1],
                log_valid["elbo_valid"][-1]
            )
        )
        print('=' * 69)
        return log_valid

    log = {'loss': [], 'kl': [], 'elbo': [], 'alpha': []}
    log_valid = {"loss_valid": [], "kl_valid": [], "elbo_valid": []}

    try:
        # save config
        config_dict = {
            "DATA_DIR": args['data_dir'],
            "X_TRAIN": args['X_train'],
            "W_TRAIN": args.get('w_train', ''),
            "X_VALID": args.get('X_valid', ''),
            "W_VALID": args.get('w_valid', ''),
            "EPOCH": args.get('epoch', 200),
            "HIDDEN": args.get('hidden', 128),
            "TR_WIDE": TR_LEN,
            "S_WIDE": S_LEN,
            "P_WIDE": P_LEN,
            "BATCH_SIZE": args.get('batch_size', 8),
            # "DROPOUT_RATE": args.get('dropout_rate', 0.0),
            "ONLY_TRAINING": args.get('only_training', False),
            "EARLY_STOPPING": args.get('use_early_stopping', False),
            "EARLY_STOPPING_THRESHOLD": args.get('tolerance', 3),
            "LEARNING_RATE": args.get('learning_rate', 1e-3),
            "PRINT_EVERY": args.get('print_every', 20),
            "USE_ANNEAL": args.get('use_anneal', False),
            "ANNEAL_SATURATE_RATE": args.get('anneal_saturate_rate', 0.4),
            "ANNEAL_RATE": args.get('anneal_rate', 1),
            # "USE_SHUFFLE": args.get('use_shuffle', False),
            "CLIP": args.get('clip', 20),
            "Z_DIM": args.get('z_dim', 16),

            "STRIDE": args.get('stride', 1),
            "KER1": args.get('ker1', 5),
            "CH1": args.get('ch1', 5),
            "KER2": args.get('ker2', 5),
            "CH2": args.get('ch2', 5),
            "KER3": args.get('ker3', 7),
            "CH3": args.get('ch3', 8),

            "BETA": args.get('beta', 0.001),
            "BETA_SUM": beta_sum_batch,
            "CKPT_ITER": args.get('ckpt_iter', 3),
            "SUFFIX": args.get('suffix', ''),
            "LOG": args.get('log', False),
            "LOG_DIR": args.get('log_dir', ''),
            "PRINT_EVERY": args.get('print_every', 20)
        }
        pprint(config_dict)
        if args.get('log'):
            with open(os.path.join(args['log_dir'], f"config{args.get('suffix', '')}.yaml"), "w") as f:
                yaml.dump(config_dict, f)

        for epoch in range(1, args.get('epoch', 200) + 1):
            print('-' * 90)
            print('Epoch {}/{}'.format(epoch, args.get('epoch', 200)))
            print('-' * 90)
            model.train()
            log = train_model(log)

            if not args.get('only_training', False) and valid_dataset:
                model.eval()
                with torch.no_grad():
                    log_valid = valid_model(log_valid)

            # save model
            if args.get('save_ckpt', False):
                if epoch % args.get('ckpt_iter', 3) == 0:
                    save_model(model, args['log_dir'], f'model_epoch{epoch}{args.get("suffix", "")}.pt')

            # early stopping
            if args.get('use_early_stopping', False) and (epoch > args.get('tolerance', 3)) and valid_dataset:
                elbo_diff = np.diff(np.array(log_valid['elbo_valid'][-args.get('tolerance', 3) - 1:]))
                if all(elbo_diff > 0):
                    print(f'Early stopping at epoch {epoch}')
                    break

        # save final version
        if args.get('log'):
            save_model(model, args['log_dir'], f'model_epoch{epoch}{args.get("suffix", "")}.pt')
            write_csv(log, args['log_dir'], f'log{args.get("suffix", "")}.csv')
            if not args.get('only_training', False) and valid_dataset:
                write_csv(log_valid, args['log_dir'], f'log_valid{args.get("suffix", "")}.csv')

    except KeyboardInterrupt:
        print('-' * 90)
        print('Exiting training early')
        print('-' * 90)
